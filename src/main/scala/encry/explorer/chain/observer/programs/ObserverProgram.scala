package encry.explorer.chain.observer.programs

import cats.Parallel
import cats.effect.concurrent.Ref
import cats.effect.{ Concurrent, Sync, Timer }
import cats.syntax.flatMap._
import cats.syntax.functor._
import encry.explorer.chain.observer.http.api.models.HttpApiBlock
import encry.explorer.chain.observer.programs.UrlsManager.UrlCurrentState
import encry.explorer.chain.observer.services.{ ClientService, GatheringService }
import encry.explorer.core.UrlAddress
import encry.explorer.core.services.DBReaderService
import encry.explorer.core.settings.ExplorerSettings
import encry.explorer.events.processing.ExplorerEvent
import fs2.Stream
import fs2.concurrent.Queue
import io.chrisdavenport.log4cats.Logger
import org.http4s.client.Client

trait ObserverProgram[F[_]] {
  def run: Stream[F, Unit]
}

object ObserverProgram {
  def apply[F[_]: Sync: Logger: Timer: Parallel: Concurrent](
    client: Client[F],
    dbReaderService: DBReaderService[F],
    blocksMarkAsNonBest: Queue[F, String],
    bestChainBlocks: Queue[F, HttpApiBlock],
    eventsQueue: Queue[F, ExplorerEvent],
    initialExplorerHeight: Int,
    ES: ExplorerSettings
  ): F[ObserverProgram[F]] =
    for {
      unreachableUrlsQueue <- Queue.bounded[F, UrlAddress](ES.httpClientSettings.maxConnections * 2)
      urlStatisticQueue    <- Queue.bounded[F, UrlCurrentState](ES.httpClientSettings.maxConnections * 2)
      isChainSyncedRef     <- Ref.of[F, Boolean](false)
      clientService        = ClientService(client)
      _                    <- Logger[F].info(s"Client service initialized successfully.")
      gatheringService     = GatheringService(clientService, unreachableUrlsQueue)
      _                    <- Logger[F].info(s"Gathering service initialized successfully.")
      urlsManager <- UrlsManager(
                      clientService,
                      gatheringService,
                      unreachableUrlsQueue,
                      urlStatisticQueue,
                      eventsQueue,
                      ES
                    )
      _               <- Logger[F].info(s"Urls manager initialized successfully.")
      networkObserver = NetworkObserver(clientService, gatheringService, urlsManager, urlStatisticQueue)
      _               <- Logger[F].info(s"Network observer initialized successfully.")
      forkResolver = ForkResolver(
        gatheringService,
        clientService,
        dbReaderService,
        urlsManager,
        isChainSyncedRef,
        unreachableUrlsQueue,
        bestChainBlocks,
        blocksMarkAsNonBest,
        eventsQueue
      )
      _ <- Logger[F].info(s"Fork resolver initialized successfully.")
      bestChainDownloader = BestChainDownloader(
        gatheringService,
        urlsManager,
        clientService,
        bestChainBlocks,
        eventsQueue,
        isChainSyncedRef,
        initialExplorerHeight
      )
      _ <- Logger[F].info(s"Best chain downloader initialized successfully.")
    } yield new ObserverProgram[F] {
      override def run: Stream[F, Unit] =
        urlsManager.run concurrently
          forkResolver.run concurrently
          bestChainDownloader.run concurrently
          networkObserver.run
    }
}
