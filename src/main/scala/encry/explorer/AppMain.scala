package encry.explorer

import java.util.concurrent.{ Executors, ThreadFactory }

import cats.effect.{ ExitCode, IO, IOApp, Resource }
import cats.syntax.applicative._
import cats.syntax.functor._
import cats.~>
import com.google.common.util.concurrent.ThreadFactoryBuilder
import doobie.free.connection.ConnectionIO
import doobie.hikari.HikariTransactor
import encry.explorer.chain.observer.http.api.models.HttpApiBlock
import encry.explorer.chain.observer.programs.NetworkObserver
import encry.explorer.core.db.DB
import encry.explorer.core.db.algebra.LiftConnectionIO
import encry.explorer.core.db.repositories.{
  HeaderRepository,
  InputRepository,
  OutputRepository,
  TransactionRepository
}
import encry.explorer.core.services.{ DBService, SettingsReader }
import encry.explorer.core.settings.ExplorerSettings
import fs2.concurrent.Queue
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder

import scala.concurrent.{ ExecutionContext, ExecutionContextExecutor }

object AppMain extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    resources.use {
      case (client, ht, sr) =>
        (for {
          implicit0(logger: SelfAwareStructuredLogger[IO]) <- Slf4jLogger.create[IO]
          implicit0(liftIO: LiftConnectionIO[IO]) <- new LiftConnectionIO[IO] {
                                                      override def liftOp: ConnectionIO ~> IO = ht.trans
                                                      override def liftF[T](v: ConnectionIO[T]): IO[T] =
                                                        liftOp.apply(v)
                                                    }.pure[IO]
          bestChainBlocks <- Queue.bounded[IO, HttpApiBlock](200)
          forkBlocks      <- Queue.bounded[IO, String](200)
          db <- DBService
                 .apply[IO](
                   bestChainBlocks,
                   forkBlocks,
                   HeaderRepository.apply[IO],
                   InputRepository.apply[IO],
                   OutputRepository.apply[IO],
                   TransactionRepository.apply[IO]
                 )
                 .pure[IO]
          dbHeight <- db.getBestHeightFromDB
          _        <- logger.info(s"Explorer app has been started. Last height in the data base is: $dbHeight.")
          no       <- NetworkObserver.apply[IO](client, bestChainBlocks, forkBlocks, sr, dbHeight)
          _        <- (no.run concurrently db.run).compile.drain
        } yield ()).as(ExitCode.Success)
    }

  def resources: Resource[IO, (Client[IO], HikariTransactor[IO], ExplorerSettings)] =
    for {
      settings <- Resource.liftF(SettingsReader.read[IO])
      tf: ThreadFactory = new ThreadFactoryBuilder()
        .setNameFormat("http-api-thread-pool-%d")
        .setDaemon(false)
        .setPriority(Thread.NORM_PRIORITY)
        .build()
      ec: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newCachedThreadPool(tf))
      client                       <- BlazeClientBuilder[IO](ec).resource
      ht                           <- DB[IO](settings)
    } yield (client, ht, settings)
}
