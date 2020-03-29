package encry.explorer.chain.observer.programs

import cats.Parallel
import cats.effect.concurrent.Ref
import cats.effect.{ ConcurrentEffect, Timer }
import cats.instances.try_._
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import encry.explorer.chain.observer.http.api.models.HttpApiBlock
import encry.explorer.chain.observer.services.{ GatheredInfoProcessor, NodeObserver }
import encry.explorer.core.settings.ExplorerSettings
import encry.explorer.core.{ HeaderHeight, Id, RunnableProgram, UrlAddress }
import fs2.Stream
import fs2.concurrent.Queue
import io.chrisdavenport.log4cats.Logger
import org.http4s.client.Client

import scala.concurrent.duration._
import scala.util.Try

trait NetworkObserver[F[_]] extends RunnableProgram[F] {
  def run: Stream[F, Unit]
}

object NetworkObserver {

  def apply[F[_]: Timer: Logger: ConcurrentEffect: Parallel](
    client: Client[F],
    queue: Queue[F, HttpApiBlock],
    SR: ExplorerSettings
  ): F[NetworkObserver[F]] =
    for {
      nodesObserver <- NodeObserver.apply[F](client).pure[F]
      ref <- Ref.of[F, List[UrlAddress]](
              SR.httpClientSettings.encryNodes.map(UrlAddress.fromString[Try](_).get)
            )
      lastRollbackIds       <- Ref.of[F, List[Id]](List.empty[Id])
      gatheredInfoProcessor <- GatheredInfoProcessor.apply[F](ref, client, nodesObserver).pure[F]
    } yield
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
