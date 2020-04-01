package encry.explorer.chain.observer.programs

import cats.Parallel
import cats.data.Chain
import cats.effect.concurrent.{ MVar, Ref }
import cats.effect.{ ConcurrentEffect, Timer }
import cats.instances.try_._
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import encry.explorer.chain.observer.http.api.models.HttpApiBlock
import encry.explorer.chain.observer.services.{ GatheredInfoProcessor, NodeObserver }
import encry.explorer.core.constants._
import encry.explorer.core.settings.ExplorerSettings
import encry.explorer.core.{ HeaderHeight, RunnableProgram, UrlAddress }
import fs2.Stream
import fs2.concurrent.Queue
import io.chrisdavenport.log4cats.Logger
import io.estatico.newtype.macros.newtype
import org.http4s.client.Client

import scala.concurrent.duration._
import scala.util.Try

trait NetworkObserver[F[_]] extends RunnableProgram[F] {
  def run: Stream[F, Unit]
}

object NetworkObserver {

  def apply[F[_]: Timer: Logger: ConcurrentEffect: Parallel](
    client: Client[F],
    bestChainBlocks: Queue[F, HttpApiBlock],
    forkBlocks: Queue[F, String],
    SR: ExplorerSettings,
    startWith: Int
  ): F[NetworkObserver[F]] =
    for {
      nodesObserver <- NodeObserver.apply[F](client).pure[F]
      ref <- Ref.of[F, List[UrlAddress]](
              SR.httpClientSettings.encryNodes.map(UrlAddress.fromString[Try](_).get)
            )
      idsInRollbackRange    <- Ref.of[F, List[String]](List.empty[String])
      gatheredInfoProcessor <- GatheredInfoProcessor.apply[F](ref, client, nodesObserver).pure[F]
      isExplorerSynced      <- MVar.of[F, Boolean](false)
    } yield
      new NetworkObserver[F] {
        override def run: Stream[F, Unit] = Stream.eval(getActualInfo(startWith))

        private def getActualInfo(workingHeight: Int): F[Unit] =
          (for {
            bestNetworkHeight <- gatheredInfoProcessor.getFullChainHeight
            _ <- Logger[F].info(
                  s"Current network best height is: $bestNetworkHeight. Current explorer height is: $workingHeight."
                )
            lastHeight <- if (bestNetworkHeight.exists(_ - RollBackHeight < workingHeight))
                           Logger[F].info(s"Going to compute potential forks.") >>
                             tryToResolveForks(workingHeight)
                         else
                           Logger[F].info(s"Going to get next block at height $workingHeight.") >>
                             getNextAvailable(workingHeight)
            _ <- tryToSetChainAsSynced(bestNetworkHeight, lastHeight)
            _ <- Logger[F].info(s"Function tryToSetChainAsSynced finished.")
          } yield lastHeight).flatMap { explorerHeight =>
            for {
              currentChainStatus <- isExplorerSynced.read
              _ <- if (currentChainStatus)
                    Logger[F].info(
                      s"Explorer is synced with network. Going to sleep 10 seconds before next activity."
                    ) >> Timer[F].sleep(10.seconds) >> getActualInfo(explorerHeight)
                  else
                    Logger[F].info(
                      s"Explorer is not synced with network. Going to sleep 0.3 seconds before next activity."
                    ) >> Timer[F].sleep(0.3.seconds) >> getActualInfo(explorerHeight)
            } yield ()
          }

        private def tryToSetChainAsSynced(networkHeight: Option[Int], explorerHeight: Int): F[Unit] =
          for {
            currentStatus <- isExplorerSynced.read
            _ <- if (!currentStatus && networkHeight.contains(explorerHeight))
                  Logger[F].info(s"Explorer is synced with network. Going to set up isSynced param into the true.") >>
                    isExplorerSynced.take >> isExplorerSynced.put(true)
                else ().pure[F]
          } yield ()

        private def tryToResolveForks(workingHeight: Int): F[Int] =
          for {
            lastNetworkIds  <- gatheredInfoProcessor.getIdsInRollbackRange(workingHeight, RollBackHeight)
            _               <- Logger[F].info(s"Last network ids are: ${lastNetworkIds.mkString(",")}.")
            explorerLastIds <- idsInRollbackRange.get
            _               <- Logger[F].info(s"Last explorer ids are: ${explorerLastIds.mkString(",")}.")
            forks           = computeForks(explorerLastIds, lastNetworkIds).toList
            height <- if (forks.isEmpty)
                       Logger[F].info(
                         s"There are no forks in network. Going to get next block at height: $workingHeight"
                       ) >> getNextAvailable(workingHeight)
                     else resolveForks(forks) >> workingHeight.pure[F]
          } yield height

        private def resolveForks(forks: List[(ExplorerId, NetworkId)]): F[Unit] =
          for {
            _ <- Logger[F].info(
                  s"There are some forks in network. They are: ${forks.mkString(",")}. Going to resolve forks."
                )
            blocks <- gatheredInfoProcessor.getBlocksByIdsMany(forks.map(_._2.value))
            _ <- if (blocks.size != forks.size)
                  Logger[F].info(s"Rolled back failed. Going to sleep 10 seconds before next request.") >>
                    Timer[F].sleep(10.seconds) >> Logger[F].info(s"Going to continue working after sleeping.")
                else
                  (bestChainBlocks.enqueue(Stream.emits(blocks)) >>
                    forkBlocks.enqueue(Stream.emits(forks.map(_._1.value)))).compile.drain >>
                    idsInRollbackRange.update(_.reverse.foldLeft(List.empty[String]) {
                      case (acc, nextId) =>
                        forks.find { case (explorerId, _) => explorerId.value == nextId } match {
                          case Some((_, networkId)) => networkId.value :: acc
                          case None                 => nextId :: acc
                        }
                    })
          } yield ()

        private def getNextAvailable(currentHeight: Int): F[Int] =
          for {
            blockOpt <- gatheredInfoProcessor.getBestBlockAt(HeaderHeight(currentHeight))
            newHeight <- blockOpt match {
                          case Some(block) =>
                            bestChainBlocks.enqueue1(block) >>
                              idsInRollbackRange.update(_.drop(1) :+ block.header.id.getValue) >>
                              (currentHeight + 1).pure[F]
                          case _ => currentHeight.pure[F]
                        }
          } yield newHeight

        private def computeForks(
          explorerIds: List[String],
          networkIds: List[String]
        ): Chain[(ExplorerId, NetworkId)] =
          if (explorerIds.size != networkIds.size) Chain.empty[(ExplorerId, NetworkId)]
          else
            explorerIds.zip(networkIds).foldLeft(Chain.empty[(ExplorerId, NetworkId)]) {
              case (changeIsNeeded, (explorerId, networkId)) if explorerId != networkId =>
                changeIsNeeded :+ (ExplorerId(explorerId) -> NetworkId(networkId))
              case (changeIsNeeded, _) => changeIsNeeded
            }

      }

  @newtype final case class ExplorerId(value: String)
  @newtype final case class NetworkId(value: String)

}
