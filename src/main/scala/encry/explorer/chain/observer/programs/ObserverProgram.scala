package encry.explorer.chain.observer.programs

import cats.Parallel
import cats.effect.concurrent.Ref
import cats.effect.{ Concurrent, Timer }
import cats.syntax.flatMap._
import cats.syntax.functor._
import encry.explorer.chain.observer.programs.UrlsManager.UrlCurrentState
import encry.explorer.chain.observer.services.{ ClientService, GatheringService }
import encry.explorer.core.UrlAddress
import encry.explorer.core.db.algebra.LiftConnectionIO
import encry.explorer.core.services.DBReaderService
import encry.explorer.env.{ HasCoreContext, HasExplorerContext, HasHttpApiContext }
import fs2.Stream
import fs2.concurrent.Queue

trait ObserverProgram[F[_]] {
  def run: Stream[F, Unit]
}

object ObserverProgram {
  def apply[F[_]: Timer: Parallel: Concurrent, CI[_]: LiftConnectionIO](
    initialExplorerHeight: Int
  )(implicit cc: HasHttpApiContext[F], ec: HasExplorerContext[F], coreC: HasCoreContext[F, CI]): F[ObserverProgram[F]] =
    for {
      isChainSyncedRef     <- Ref.of[F, Boolean](false)
      client               <- cc.ask(_.client)
      logger               <- ec.ask(_.logger)
      settings             <- ec.ask(_.settings)
      unreachableUrlsQueue <- Queue.bounded[F, UrlAddress](settings.httpClientSettings.maxConnections * 2)
      urlStatisticQueue    <- Queue.bounded[F, UrlCurrentState](settings.httpClientSettings.maxConnections * 2)
      clientService        = ClientService(client)
      _                    <- logger.info(s"Client service initialized successfully.")
      gatheringService     = GatheringService(clientService, unreachableUrlsQueue)
      _                    <- logger.info(s"Gathering service initialized successfully.")
      urlsManager          <- UrlsManager(clientService, gatheringService, unreachableUrlsQueue, urlStatisticQueue)
      _                    <- logger.info(s"Urls manager initialized successfully.")
      networkObserver      = NetworkObserver(clientService, gatheringService, urlsManager, urlStatisticQueue)
      _                    <- logger.info(s"Network observer initialized successfully.")
      dbReaderService      = DBReaderService[F, CI]
      forkResolver = ForkResolver(
        gatheringService,
        clientService,
        dbReaderService,
        urlsManager,
        isChainSyncedRef
      )
      _ <- logger.info(s"Fork resolver initialized successfully.")
      bestChainDownloader = BestChainDownloader(
        gatheringService,
        urlsManager,
        clientService,
        isChainSyncedRef,
        initialExplorerHeight
      )
      _ <- logger.info(s"Best chain downloader initialized successfully.")
    } yield new ObserverProgram[F] {
      override def run: Stream[F, Unit] =
        urlsManager.run concurrently
          forkResolver.run concurrently
          bestChainDownloader.run concurrently
          networkObserver.run
    }
}
