package encry.explorer.events.processing

import cats.effect.{ ConcurrentEffect, ContextShift, Timer }
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.traverse._
import encry.explorer.env.HasExplorerContext
import fs2.kafka._
import fs2.{ Chunk, Stream }

import scala.concurrent.duration._

trait EventsProducer[F[_]] {
  def runProducer: Stream[F, Unit]
}

object EventsProducer {
  def apply[F[_]: ConcurrentEffect: ContextShift: Timer](
    implicit explorerContext: HasExplorerContext[F]
  ): EventsProducer[F] =
    new EventsProducer[F] {
      override def runProducer: Stream[F, Unit] = kafkaProducer.void

      private val producerSettings: Stream[F, ProducerSettings[F, String, ExplorerEvent]] =
        Stream.eval(explorerContext.ask(_.settings).map { ES =>
          ProducerSettings(
            keySerializer = Serializer[F, String],
            valueSerializer = explorerEventKafkaSerializer.createSerializer[F]
          ).withBootstrapServers(ES.kafkaSettings.consumerAddress)
        })

      private def kafkaProducer: Stream[F, Chunk[ProducerResult[String, ExplorerEvent, Unit]]] =
        producerSettings.flatMap(producerStream(_)).flatMap { producer =>
          Stream
            .eval(
              for {
                eq     <- explorerContext.ask(_.sharedQueuesContext.eventsQueue)
                logger <- explorerContext.ask(_.logger)
              } yield (eq, logger)
            )
            .flatMap {
              case (eq, logger) =>
                eq.dequeue
                  .evalTap(event => logger.info(s"Going to produce new explorer event: $event."))
                  .map(newEvent =>
                    ProducerRecords.one(ProducerRecord(newEvent.kafkaTopic, newEvent.kafkaKey, newEvent))
                  )
                  .evalTap(_ => logger.info(s"New event created"))
                  .evalMap(producer.produce)
                  .groupWithin(20, 5.seconds)
                  .evalMap(_.sequence)
                  .evalTap(_ => logger.info(s"New explorer event produced."))
                  .handleErrorWith { err =>
                    Stream.eval(logger.info(s"Error ${err.getMessage} has occurred while processing kafka producer")) >>
                      kafkaProducer
                  }
            }
        }
    }
}
