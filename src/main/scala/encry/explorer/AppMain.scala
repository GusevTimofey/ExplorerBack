package encry.explorer

import java.util.concurrent.{ Executors, ThreadFactory }

import cats.effect.{ ExitCode, IO, IOApp, Resource }
import cats.syntax.functor._
import cats.~>
import com.google.common.util.concurrent.ThreadFactoryBuilder
import doobie.free.connection.ConnectionIO
import doobie.hikari.HikariTransactor
import encry.explorer.chain.observer.http.api.models.HttpApiBlock
import encry.explorer.chain.observer.programs.ObserverProgram
import encry.explorer.core.db.DB
import encry.explorer.core.db.algebra.LiftConnectionIO
import encry.explorer.core.db.repositories.{
  HeaderRepository,
  InputRepository,
  OutputRepository,
  TransactionRepository
}
import encry.explorer.core.services.{ DBReaderService, DBService, SettingsReader }
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
          implicit0(liftIO: LiftConnectionIO[IO]) = new LiftConnectionIO[IO] {
            override def liftOp: ConnectionIO ~> IO          = ht.trans
            override def liftF[T](v: ConnectionIO[T]): IO[T] = liftOp.apply(v)
          }
          _                <- logger.info(s"Resources and implicit values were initialised successfully.")
          bestChainBlocks  <- Queue.bounded[IO, HttpApiBlock](sr.encrySettings.rollbackMaxHeight * 2)
          forkBlocks       <- Queue.bounded[IO, String](sr.encrySettings.rollbackMaxHeight)
          (hr, ir, or, tr) = repositories
          _                <- logger.info(s"All repositories were initialised successfully.")
          dbReader         = DBReaderService[IO](hr)
          _                <- logger.info(s"DB reader was initialised successfully.")
          db               = DBService[IO](bestChainBlocks, forkBlocks, hr, ir, or, tr)
          _                <- logger.info(s"DB service was created successfully.")
          dbHeight         <- db.getBestHeightFromDB
          _                <- logger.info(s"Last height in the explorer DB is: $dbHeight.")
          op               <- ObserverProgram[IO](client, dbReader, forkBlocks, bestChainBlocks, dbHeight, sr)
          _                <- logger.info(s"Chain observer program stated successfully.")
          _                <- (op.run concurrently db.run).compile.drain
          _                <- logger.info(s"Explorer app has been started. Last height in the data base is: $dbHeight.")
        } yield ()).as(ExitCode.Success)
    }

  private def resources: Resource[IO, (Client[IO], HikariTransactor[IO], ExplorerSettings)] =
    for {
      settings <- Resource.liftF(SettingsReader.read[IO])
      tf: ThreadFactory = new ThreadFactoryBuilder()
        .setNameFormat("http-api-thread-pool-%d")
        .setDaemon(false)
        .setPriority(Thread.NORM_PRIORITY)
        .build()
      ec: ExecutionContextExecutor = ExecutionContext.fromExecutor(
        Executors.newFixedThreadPool(settings.httpClientSettings.observerClientThreadsQuantity, tf)
      )
      client <- BlazeClientBuilder[IO](ec).resource
      ht     <- DB[IO](settings)
    } yield (client, ht, settings)

  private def repositories(
    implicit liftIO: LiftConnectionIO[IO]
  ): (HeaderRepository[IO], InputRepository[IO], OutputRepository[IO], TransactionRepository[IO]) =
    (HeaderRepository[IO], InputRepository[IO], OutputRepository[IO], TransactionRepository[IO])
}
