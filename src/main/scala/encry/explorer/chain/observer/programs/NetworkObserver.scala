package encry.explorer.chain.observer.programs

import cats.{ ApplicativeError, Parallel }
import cats.data.Chain
import cats.effect.concurrent.Ref
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
    } yield
      new NetworkObserver[F] {
        override def run: Stream[F, Unit] = Stream.eval(getActualInfo(startWith))

        private def getActualInfo(workingHeight: Int): F[Unit] =
          (for {
            lastNetworkIds  <- gatheredInfoProcessor.getIdsInRollbackRange(workingHeight, RollBackHeight)
            explorerLastIds <- idsInRollbackRange.get
            forks           = computeForks(explorerLastIds, lastNetworkIds).toList
            lastHeight <- if (forks.nonEmpty) resolveForks(forks) >> workingHeight.pure[F]
                          else getNextAvailable(workingHeight)
          } yield lastHeight)
            .flatMap(h => Timer[F].sleep(0.5.seconds) >> getActualInfo(h))

        private def resolveForks(forks: List[(ExplorerId, NetworkId)])(
          implicit G: ApplicativeError[F, Throwable]
        ): F[Unit] =
          for {
            blocks <- gatheredInfoProcessor.getBlocksByIdsMany(forks.map(_._2.value))
            _ <- if (blocks.size != forks.size) G.raiseError[Unit](new Throwable("Rolled back failed"))
                else
                  (bestChainBlocks.enqueue(Stream.emits(blocks)) >>
                    forkBlocks.enqueue(Stream.emits(forks.map(_._1.value)))).compile.drain
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
