package encry.explorer.events.processing

import cats.effect.{ ConcurrentEffect, ContextShift, Timer }
import cats.syntax.functor._
import cats.syntax.traverse._
import encry.explorer.core.settings.ExplorerSettings
import fs2.{ Chunk, Stream }
import fs2.concurrent.Queue
import fs2.kafka._
import io.chrisdavenport.log4cats.Logger

import scala.concurrent.duration._

trait EventsProducer[F[_]] {
  def runProducer: Stream[F, Unit]
}

object EventsProducer {
  def apply[F[_]: ConcurrentEffect: ContextShift: Logger: Timer](
    eventsQueue: Queue[F, ExplorerEvent],
    ES: ExplorerSettings
  ): EventsProducer[F] = new EventsProducer[F] {
    override def runProducer: Stream[F, Unit] = kafkaProducer.void

    private val producerSettings: ProducerSettings[F, String, ExplorerEvent] = ProducerSettings(
      keySerializer = Serializer[F, String],
      valueSerializer = explorerEventKafkaSerializer.createSerializer[F]
    ).withBootstrapServers(ES.kafkaSettings.consumerAddress)

    private def kafkaProducer: Stream[F, Chunk[ProducerResult[String, ExplorerEvent, Unit]]] =
      producerStream(producerSettings).flatMap { producer =>
        eventsQueue.dequeue
          .evalTap(event => Logger[F].info(s"Going to produce new explorer event: $event."))
          .map(newEvent => ProducerRecords.one(ProducerRecord(newEvent.kafkaTopic, newEvent.kafkaKey, newEvent)))
          .evalTap(_ => Logger[F].info(s"New event created"))
          .evalMap(producer.produce)
          .groupWithin(20, 5.seconds)
          .evalMap(_.sequence)
          .evalTap(_ => Logger[F].info(s"New explorer event produced."))
      }.handleErrorWith { err =>
        Stream.eval(Logger[F].info(s"Error ${err.getMessage} has occurred while processing kafka producer")) >>
          kafkaProducer
      }
  }
}
