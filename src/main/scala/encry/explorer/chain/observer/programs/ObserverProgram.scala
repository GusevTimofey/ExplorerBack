package encry.explorer.chain.observer.programs

import cats.Parallel
import cats.effect.concurrent.Ref
import cats.effect.{Concurrent, Sync, Timer}
import cats.syntax.flatMap._
import cats.syntax.functor._
import encry.explorer.chain.observer.http.api.models.HttpApiBlock
import encry.explorer.chain.observer.services.UrlsManagerService.UrlCurrentState
import encry.explorer.chain.observer.services.{ClientService, GatheringService, UrlsManagerService}
import encry.explorer.core.services.DBReaderService
import encry.explorer.core.settings.ExplorerSettings
import encry.explorer.core.{RunnableProgram, UrlAddress}
import fs2.Stream
import fs2.concurrent.Queue
import io.chrisdavenport.log4cats.Logger
import org.http4s.client.Client

trait ObserverProgram[F[_]] extends RunnableProgram[F] {
  def run: Stream[F, Unit]
}

object ObserverProgram {
  def apply[F[_]: Sync: Logger: Timer: Parallel: Concurrent](
    client: Client[F],
    dbReaderService: DBReaderService[F],
    blocksMarkAsNonBest: Queue[F, String],
    bestChainBlocks: Queue[F, HttpApiBlock],
    initialExplorerHeight: Int,
    SR: ExplorerSettings
  ): F[ObserverProgram[F]] =
    for {
      incomingUnreachableUrlsQueue <- Queue.bounded[F, UrlAddress](100)
      incomingUrlStatisticQueue    <- Queue.bounded[F, UrlCurrentState](100)
      isChainSyncedRef             <- Ref.of[F, Boolean](false)
      clientService                = ClientService(client)
      gatheringService             = GatheringService(clientService, incomingUnreachableUrlsQueue)
      urlsManagerService           <- UrlsManagerService(incomingUnreachableUrlsQueue, incomingUrlStatisticQueue, SR)
      forkResolver = ForkResolver(
        gatheringService,
        clientService,
        dbReaderService,
        urlsManagerService,
        isChainSyncedRef,
        incomingUnreachableUrlsQueue,
        bestChainBlocks,
        blocksMarkAsNonBest
      )
      bestChainDownloader = BestChainDownloader(
        gatheringService,
        urlsManagerService,
        clientService,
        bestChainBlocks,
        isChainSyncedRef,
        initialExplorerHeight
      )
    } yield
      new ObserverProgram[F] {
        override def run: Stream[F, Unit] =
          urlsManagerService.run concurrently forkResolver.run concurrently bestChainDownloader.run
      }
}
