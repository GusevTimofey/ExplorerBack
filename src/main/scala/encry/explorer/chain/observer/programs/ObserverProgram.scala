package encry.explorer.chain.observer.programs

import cats.Parallel
import cats.effect.concurrent.Ref
import cats.effect.{ Concurrent, Timer }
import cats.syntax.flatMap._
import cats.syntax.functor._
import encry.explorer.chain.observer.services.{ ClientService, GatheringService }
import encry.explorer.core.services.DBReaderService
import encry.explorer.core.settings.ExplorerSettingsContext
import encry.explorer.env.{ ContextClientQueues, ContextHttpClient, ContextSharedQueues }
import fs2.Stream
import io.chrisdavenport.log4cats.Logger

trait ObserverProgram[F[_]] {
  def run: Stream[F, Unit]
}

object ObserverProgram {
  def apply[F[_]: Timer: Parallel: Concurrent: ContextSharedQueues: ContextClientQueues: Logger](
    dbReaderService: DBReaderService[F],
    initialExplorerHeight: Int,
    ES: ExplorerSettingsContext
  )(implicit clientC: ContextHttpClient[F]): F[ObserverProgram[F]] =
    for {

      isChainSyncedRef <- Ref.of[F, Boolean](false)
      clientService    <- clientC.ask(c => ClientService(c.client))
      _                <- Logger[F].info(s"Client service initialized successfully.")
      gatheringService <- GatheringService(clientService)
      _                <- Logger[F].info(s"Gathering service initialized successfully.")
      urlsManager      <- UrlsManager(clientService, gatheringService, ES)
      _                <- Logger[F].info(s"Urls manager initialized successfully.")
      networkObserver  <- NetworkObserver(clientService, gatheringService, urlsManager)
      _                <- Logger[F].info(s"Network observer initialized successfully.")
      forkResolver <- ForkResolver(
                       gatheringService,
                       clientService,
                       dbReaderService,
                       urlsManager,
                       isChainSyncedRef
                     )
      _ <- Logger[F].info(s"Fork resolver initialized successfully.")
      bestChainDownloader <- BestChainDownloader(
                              gatheringService,
                              urlsManager,
                              clientService,
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
