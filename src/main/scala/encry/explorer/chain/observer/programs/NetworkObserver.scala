package encry.explorer.chain.observer.programs

import cats.effect.{ ConcurrentEffect, Timer }
import cats.syntax.flatMap._
import cats.syntax.functor._
import encry.explorer.chain.observer.http.api.models.HttpApiBlock
import encry.explorer.chain.observer.services.GatheredInfoProcessor
import encry.explorer.core.services.SettingsReader
import encry.explorer.core.{ HeaderHeight, RunnableProgram }
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
    SR: SettingsReader[F],
    gatheredInfoProcessor: GatheredInfoProcessor[F]
  ): NetworkObserver[F] =
    new NetworkObserver[F] {
      override def run: Stream[F, Unit] = Stream.eval(getActualInfo(0))

      private def getActualInfo(initHeight: Int): F[Unit] =
        for {
          blockOpt <- gatheredInfoProcessor.getBestBlockAt(HeaderHeight(initHeight))
          _ <- blockOpt match {
                case Some(block) =>
                  queue.enqueue1(block) >> Timer[F].sleep(0.5.seconds) >> getActualInfo(initHeight + 1)
                case None =>
                  Timer[F].sleep(0.5.seconds) >> getActualInfo(initHeight)
              }
        } yield ()

    }

}
