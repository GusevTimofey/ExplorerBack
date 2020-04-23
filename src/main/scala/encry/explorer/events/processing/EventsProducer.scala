package encry.explorer.events.processing

import cats.effect.{ ConcurrentEffect, ContextShift, Timer }
import cats.syntax.functor._
import cats.syntax.flatMap._
import cats.syntax.traverse._
import encry.explorer.core.settings.ExplorerSettings
import encry.explorer.env.{ ContextLogging, ContextSharedQueues }
import fs2.kafka._
import fs2.{ Chunk, Stream }
import io.chrisdavenport.log4cats.Logger

import scala.concurrent.duration._

trait EventsProducer[F[_]] {
  def runProducer: Stream[F, Unit]
}

object EventsProducer {
  def apply[F[_]: ConcurrentEffect: ContextShift: Timer](
    ES: ExplorerSettings
  )(implicit sharedQueuesContext: ContextSharedQueues[F], logging: ContextLogging[F]): F[EventsProducer[F]] =
    for {
      eventsQueue <- sharedQueuesContext.ask(_.eventsQueue)
      logger      <- logging.ask(_.logger)
    } yield new EventsProducer[F] {
      override def runProducer: Stream[F, Unit] = kafkaProducer.void

      private val producerSettings: ProducerSettings[F, String, ExplorerEvent] = ProducerSettings(
        keySerializer = Serializer[F, String],
        valueSerializer = explorerEventKafkaSerializer.createSerializer[F]
      ).withBootstrapServers(ES.kafkaSettings.consumerAddress)

      private def kafkaProducer: Stream[F, Chunk[ProducerResult[String, ExplorerEvent, Unit]]] =
        producerStream(producerSettings).flatMap { producer =>
          eventsQueue.dequeue
            .evalTap(event => logger.info(s"Going to produce new explorer event: $event."))
            .map(newEvent => ProducerRecords.one(ProducerRecord(newEvent.kafkaTopic, newEvent.kafkaKey, newEvent)))
            .evalTap(_ => logger.info(s"New event created"))
            .evalMap(producer.produce)
            .groupWithin(20, 5.seconds)
            .evalMap(_.sequence)
            .evalTap(_ => logger.info(s"New explorer event produced."))
        }.handleErrorWith { err =>
          Stream.eval(logger.info(s"Error ${err.getMessage} has occurred while processing kafka producer")) >>
            kafkaProducer
        }
    }
}
