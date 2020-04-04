package encry.explorer.chain.observer.programs

import cats.data.Chain
import cats.effect.concurrent.Ref
import cats.effect.{ Sync, Timer }
import cats.syntax.applicative._
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.{ Applicative, Monad }
import encry.explorer.chain.observer.errors.HttpApiErr
import encry.explorer.chain.observer.http.api.models.HttpApiBlock
import encry.explorer.chain.observer.services.{ ClientService, GatheringService, UrlsManagerService }
import encry.explorer.core.UrlAddress
import encry.explorer.core.constants._
import encry.explorer.core.services.DBReaderService
import fs2.Stream
import fs2.concurrent.Queue
import io.estatico.newtype.macros.newtype

import scala.concurrent.duration._

trait ForkResolver[F[_]] {
  def run: Stream[F, Unit]
}

object ForkResolver {
  def apply[F[_]: Timer: Applicative: Monad: Sync](
    gatheringService: GatheringService[F],
    clientService: ClientService[F],
    dbReaderService: DBReaderService[F],
    urlsManagerService: UrlsManagerService[F],
    isChainSyncedRef: Ref[F, Boolean],
    incomingUnreachableUrls: Queue[F, UrlAddress],
    blocksToResolve: Queue[F, HttpApiBlock],
    blocksMarkAsNonBest: Queue[F, String]
  ): ForkResolver[F] = new ForkResolver[F] {

    override def run: Stream[F, Unit] =
      Stream(())
        .covary[F]
        .metered(30.seconds)
        .evalMap(_ => isChainSyncedRef.get.flatMap(if (_) resolveForks else ().pure[F]))

    private def resolveForks: F[Unit] =
      for {
        lastExplorerIds <- dbReaderService.getLastIds(RollBackHeight)
        lastDBHeight    <- dbReaderService.getBestHeight.map(_.getOrElse(0))
        urls            <- urlsManagerService.getAvailableUrls
        lastNetworkIds  <- gatheringService.gatherAll(clientService.getLastIds(lastDBHeight, RollBackHeight), urls)
        (forks, urlsForRequest) = computeMostFrequent(lastNetworkIds) match {
          case Some((ids, urls)) => computeForks(lastExplorerIds.map(_.getValue), ids) -> urls
          case None              => List.empty                                         -> List.empty
        }
        _ <- if (forks.nonEmpty) resolveForks(forks, urlsForRequest) else ().pure[F]
      } yield ()

    private def resolveForks(forks: List[(ExplorerId, NetworkId)], urlsForRequest: List[UrlAddress]): F[Unit] =
      for {
        blocks <- gatheringService.gatherMany(forks.map { ids =>
                   val f: UrlAddress => F[Either[HttpApiErr, HttpApiBlock]] = clientService.getBlockBy(ids._2.value)
                   f
                 }, urlsForRequest)
        _ <- blocksToResolve.enqueue(Stream.emits(blocks)).compile.drain
        _ <- blocksMarkAsNonBest.enqueue(Stream.emits(forks.map(_._1.value))).compile.drain
      } yield ()

    private def computeMostFrequent[R](list: List[(UrlAddress, R)]): Option[(R, List[UrlAddress])] =
      Either.catchNonFatal(list.groupBy(_._2).maxBy(_._2.size)).toOption.map {
        case (r, urlsRaw) => r -> urlsRaw.map(_._1)
      }

    private def computeForks(
      explorerIds: List[String],
      networkIds: List[String]
    ): List[(ExplorerId, NetworkId)] =
      if (explorerIds.size != networkIds.size) List.empty[(ExplorerId, NetworkId)]
      else
        explorerIds
          .zip(networkIds)
          .foldLeft(Chain.empty[(ExplorerId, NetworkId)]) {
            case (changeIsNeeded, (explorerId, networkId)) if explorerId != networkId =>
              changeIsNeeded :+ (ExplorerId(explorerId) -> NetworkId(networkId))
            case (changeIsNeeded, _) => changeIsNeeded
          }
          .toList
  }

  @newtype final case class ExplorerId(value: String)
  @newtype final case class NetworkId(value: String)

}
