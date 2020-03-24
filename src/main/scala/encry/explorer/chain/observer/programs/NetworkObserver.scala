package encry.explorer.chain.observer.programs

import cats.effect.{ Concurrent, ConcurrentEffect, Sync, Timer }
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import encry.explorer.chain.observer.http.api.models.HttpApiBlock
import encry.explorer.chain.observer.services.NodeObserver
import encry.explorer.core.{ HeaderHeight, Id, RunnableProgram, UrlAddress }
import fs2.Stream
import fs2.concurrent.Queue
import io.chrisdavenport.log4cats.Logger
import org.http4s.client.Client

import scala.concurrent.duration._

trait NetworkObserver[F[_]] extends RunnableProgram[F] {
  def run: Stream[F, Unit]
}

object NetworkObserver {

  def apply[F[_]: Sync: Timer: Concurrent: Logger: ConcurrentEffect](
    client: Client[F],
    queue: Queue[F, HttpApiBlock]
  ): NetworkObserver[F] =
    new NetworkObserver[F] {
      override def run: Stream[F, Unit] = Stream.eval(getActualInfo(0))
//        Stream(()).repeat
//          .covary[F]
//          .metered(1.seconds)
//          .evalMap(_ => queue.dequeue1.void) concurrently

      val observerService: F[NodeObserver[F]] =
        for {
          url <- UrlAddress.fromString[F]("http://172.16.11.14:9051")
          res <- NodeObserver(client, url).pure[F]
        } yield res

      def getActualInfo(initHeight: Int): F[Unit] =
        (for {
          service <- observerService
          idRaw   <- service.getBestBlockIdAt(HeaderHeight(initHeight))
          id      <- Id.fromString(idRaw.get)
          block   <- service.getBlockBy(id)
          _       <- queue.enqueue1(block.get)
          _       <- Timer[F].sleep(1.seconds)
        } yield ()) >> getActualInfo(initHeight + 1)

    }

}
