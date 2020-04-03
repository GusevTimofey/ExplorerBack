package encry.explorer.chain.observer.programs

import cats.effect.{ Sync, Timer }
import cats.effect.concurrent.Ref
import encry.explorer.chain.observer.services.{ ClientService, GatheringService, UrlsManagerService }
import encry.explorer.core.{ Id, UrlAddress }
import fs2.Stream
import cats.syntax.functor._
import cats.syntax.flatMap._
import cats.syntax.either._
import cats.syntax.applicative._
import cats.instances.try_._
import encry.explorer.chain.observer.http.api.models.HttpApiBlock
import fs2.concurrent.Queue

import scala.util.Try
import scala.concurrent.duration._

trait BestChainDownloader[F[_]] {
  def run: Stream[F, Unit]
}

object BestChainDownloader {
  def apply[F[_]: Sync: Timer](
    gatheringService: GatheringService[F],
    urlsManagerService: UrlsManagerService[F],
    clientService: ClientService[F],
    bestChainBlocks: Queue[F, HttpApiBlock],
    isChainSyncedRef: Ref[F, Boolean],
    initialExplorerHeight: Int
  ): BestChainDownloader[F] =
    new BestChainDownloader[F] {
      override def run: Stream[F, Unit] = Stream.eval(downloadNext(initialExplorerHeight))

      private def downloadNext(workingHeight: Int): F[Unit] =
        (for {
          urls                  <- urlsManagerService.getAvailableUrls
          bestIdAtWorkingHeight <- gatheringService.gatherAll(clientService.getBestBlockIdAt(workingHeight), urls)
          newHeight <- computeMostFrequentId(bestIdAtWorkingHeight) match {
                        case Some((id, urls)) =>
                          gatheringService.gatherFirst(clientService.getBlockBy(id.getValue), urls).flatMap {
                            case Some(block) => bestChainBlocks.enqueue1(block).map(_ => workingHeight + 1)
                            case None        => workingHeight.pure[F]
                          }
                        case None => workingHeight.pure[F]
                      }
          networkHeight <- gatheringService
                            .gatherAll(clientService.getBestFullHeight, urls)
                            .map(computeMostFrequent)
                            .map(_.map(_._1).getOrElse(0))
          currentChainStatus <- isChainSyncedRef.get
          _ <- if (!currentChainStatus && newHeight == networkHeight) isChainSyncedRef.set(true) else ().pure[F]
        } yield newHeight).flatMap { eh =>
          isChainSyncedRef.get.flatMap { isChainSynced =>
            if (isChainSynced) Timer[F].sleep(20.seconds) >> downloadNext(eh)
            else downloadNext(eh)
          }
        }

      private def computeMostFrequentId: List[(UrlAddress, String)] => Option[(Id, List[UrlAddress])] =
        (list: List[(UrlAddress, String)]) =>
          for {
            (id, urls) <- computeMostFrequent(list)
            correctUrl <- Id.fromString[Try](id).toOption
          } yield correctUrl -> urls

      private def computeMostFrequent[R]: List[(UrlAddress, R)] => Option[(R, List[UrlAddress])] =
        (lists: List[(UrlAddress, R)]) =>
          Either.catchNonFatal(lists.groupBy(_._2).maxBy(_._2.size)).toOption.map {
            case (r, urlsRaw) => r -> urlsRaw.map(_._1)
        }

    }
}
