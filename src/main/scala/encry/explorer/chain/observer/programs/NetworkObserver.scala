package encry.explorer.chain.observer.programs

import cats.effect.{ConcurrentEffect, Timer}
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import encry.explorer.chain.observer.http.api.models.HttpApiBlock
import encry.explorer.chain.observer.services.NodeObserver
import encry.explorer.core.services.SettingsReader
import encry.explorer.core.{HeaderHeight, Id, RunnableProgram, UrlAddress}
import fs2.Stream
import fs2.concurrent.Queue
import io.chrisdavenport.log4cats.Logger
import org.http4s.client.Client

import scala.concurrent.duration._

trait NetworkObserver[F[_]] extends RunnableProgram[F] {
  def run: Stream[F, Unit]
}

object NetworkObserver {

  def apply[F[_]: Timer: Logger: ConcurrentEffect](
    client: Client[F],
    queue: Queue[F, HttpApiBlock],
    SR: SettingsReader[F]
  ): NetworkObserver[F] =
    new NetworkObserver[F] {
      override def run: Stream[F, Unit] = Stream.eval(getActualInfo(0))

      val observerService: F[NodeObserver[F]] =
        for {
          url <- UrlAddress.fromString[F]("http://172.16.11.14:9051")
          res <- NodeObserver(client, url).pure[F]
        } yield res

      def getActualInfo(initHeight: Int): F[Unit] =
        (for {
          service <- observerService
          _ <- service.getBestFullHeight
          idRaw   <- service.getBestBlockIdAt(HeaderHeight(initHeight))
          id      <- Id.fromString(idRaw.get)
          block   <- service.getBlockBy(id)
          _       <- queue.enqueue1(block.get)
          _       <- Timer[F].sleep(1.seconds)
        } yield ()) >> getActualInfo(initHeight + 1)

    }

}
