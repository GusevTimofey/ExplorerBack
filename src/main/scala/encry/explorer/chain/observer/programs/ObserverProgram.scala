package encry.explorer.chain.observer.programs

import cats.Parallel
import cats.effect.concurrent.Ref
import cats.effect.{ Concurrent, Timer }
import cats.syntax.flatMap._
import cats.syntax.functor._
import encry.explorer.chain.observer.services.{ ClientService, GatheringService }
import encry.explorer.core.services.DBReaderService
import encry.explorer.core.settings.ExplorerSettings
import encry.explorer.env.{ ContextClientQueues, ContextHttpClient, ContextLogging, ContextSharedQueues }
import fs2.Stream
import io.chrisdavenport.log4cats.Logger
import org.http4s.client.Client

trait ObserverProgram[F[_]] {
  def run: Stream[F, Unit]
}

object ObserverProgram {
  def apply[F[_]: Timer: Parallel: Concurrent: ContextSharedQueues: ContextClientQueues](
    dbReaderService: DBReaderService[F],
    initialExplorerHeight: Int,
    ES: ExplorerSettings
  )(implicit loggingC: ContextLogging[F], clientC: ContextHttpClient[F]): F[ObserverProgram[F]] =
    for {

      isChainSyncedRef          <- Ref.of[F, Boolean](false)
      implicit0(log: Logger[F]) <- loggingC.ask(_.logger)
      clientService             <- clientC.ask(c => ClientService(c.client))
      _                         <- log.info(s"Client service initialized successfully.")
      gatheringService          <- GatheringService(clientService)
      _                         <- log.info(s"Gathering service initialized successfully.")
      urlsManager               <- UrlsManager(clientService, gatheringService, ES)
      _                         <- log.info(s"Urls manager initialized successfully.")
      networkObserver           <- NetworkObserver(clientService, gatheringService, urlsManager)
      _                         <- log.info(s"Network observer initialized successfully.")
      forkResolver <- ForkResolver(
                       gatheringService,
                       clientService,
                       dbReaderService,
                       urlsManager,
                       isChainSyncedRef
                     )
      _ <- log.info(s"Fork resolver initialized successfully.")
      bestChainDownloader <- BestChainDownloader(
                              gatheringService,
                              urlsManager,
                              clientService,
                              isChainSyncedRef,
                              initialExplorerHeight
                            )
      _ <- log.info(s"Best chain downloader initialized successfully.")
    } yield new ObserverProgram[F] {
      override def run: Stream[F, Unit] =
        urlsManager.run concurrently
          forkResolver.run concurrently
          bestChainDownloader.run concurrently
          networkObserver.run
    }
}
