package encry.explorer.chain.observer.programs

import cats.effect.concurrent.Ref
import cats.effect.{ Sync, Timer }
import cats.syntax.flatMap._
import cats.syntax.functor._
import encry.explorer.chain.observer.errors.HttpApiErr
import encry.explorer.chain.observer.http.api.models.HttpApiBlock
import encry.explorer.chain.observer.services.{ ClientService, GatheringService }
import encry.explorer.core
import fs2.Stream
import fs2.concurrent.Queue
import io.chrisdavenport.log4cats.Logger

import scala.concurrent.duration._

trait ForkDownloader[F[_]] {
  def run: Stream[F, Unit]
}

//todo implement
object ForkDownloader {
  def apply[F[_]: Sync: Timer: Logger](
    gatheringService: GatheringService[F],
    clientService: ClientService[F],
    urlsManager: UrlsManager[F],
    isChainSyncedRef: Ref[F, Boolean],
    outgoingForksBlocks: Queue[F, HttpApiBlock],
    startWithHeight: Int
  ): F[ForkDownloader[F]] =
    for {
      lastRollBackBlocks <- Ref.of[F, Map[Int, List[String]]](Map.empty)
    } yield
      new ForkDownloader[F] {
        override def run: Stream[F, Unit] = Stream.eval(getForksAtHeight(startWithHeight))

        private def getForksAtHeight(height: Int): F[Unit] =
          (for {
            urls             <- urlsManager.getAvailableUrls
            blocksToUrls     <- gatheringService.gatherAll(clientService.getBlockIdsAt(height), urls)
            allIdsForRequest = blocksToUrls.flatMap(_._2).distinct
            requests: List[core.UrlAddress => F[Either[HttpApiErr, HttpApiBlock]]] = allIdsForRequest.map(
              clientService.getBlockBy
            )
            blocks <- gatheringService.gatherOneFromMany(requests, urls)
            _      <- Logger[F].info(s"Going to send ${blocks.size} fork blocks at height $height.")
            _      <- outgoingForksBlocks.enqueue(Stream.emits(blocks)).compile.drain
          } yield if (allIdsForRequest.nonEmpty) height + 1 else height).flatMap { height =>
            isChainSyncedRef.get.flatMap { isChainSynced =>
              if (isChainSynced) Timer[F].sleep(30.seconds) >> getForksAtHeight(height)
              else getForksAtHeight(height)
            }
          }

      }
}
