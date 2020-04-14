package encry.explorer

import java.util.concurrent.{ Executors, ThreadFactory }

import cats.effect.{ ExitCode, Resource }
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
import encry.explorer.events.processing.{ EventsProducer, ExplorerEvent }
import fs2.concurrent.Queue
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import monix.eval.{ Task, TaskApp }
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder

import scala.concurrent.{ ExecutionContext, ExecutionContextExecutor }

object AppMain extends TaskApp {
  override def run(args: List[String]): Task[ExitCode] =
    resources.use {
      case (client, ht, sr) =>
        (for {
          implicit0(logger: SelfAwareStructuredLogger[Task]) <- Slf4jLogger.create[Task]
          implicit0(liftIO: LiftConnectionIO[Task]) = new LiftConnectionIO[Task] {
            override def liftOp: ConnectionIO ~> Task          = ht.trans
            override def liftF[T](v: ConnectionIO[T]): Task[T] = liftOp.apply(v)
          }
          _                <- logger.info(s"Resources and implicit values were initialised successfully.")
          bestChainBlocks  <- Queue.bounded[Task, HttpApiBlock](sr.encrySettings.rollbackMaxHeight * 2)
          forkBlocks       <- Queue.bounded[Task, String](sr.encrySettings.rollbackMaxHeight)
          (hr, ir, or, tr) = repositories
          _                <- logger.info(s"All repositories were initialised successfully.")
          dbReader         = DBReaderService[Task](hr)
          _                <- logger.info(s"DB reader was initialised successfully.")
          db               = DBService[Task](bestChainBlocks, forkBlocks, hr, ir, or, tr)
          _                <- logger.info(s"DB service was created successfully.")
          dbHeight         <- db.getBestHeightFromDB
          _                <- logger.info(s"Last height in the explorer DB is: $dbHeight.")
          eventsQueue      <- Queue.unbounded[Task, ExplorerEvent]
          ep                = EventsProducer[Task](eventsQueue, sr)
          op               <- ObserverProgram[Task](client, dbReader, forkBlocks, bestChainBlocks, eventsQueue, dbHeight, sr)
          _                <- logger.info(s"Chain observer program stated successfully.")
          _                <- (op.run concurrently db.run concurrently ep.runProducer).compile.drain
          _                <- logger.info(s"Explorer app has been started. Last height in the data base is: $dbHeight.")
        } yield ()).as(ExitCode.Success)
    }

  private def resources: Resource[Task, (Client[Task], HikariTransactor[Task], ExplorerSettings)] =
    for {
      settings <- Resource.liftF(SettingsReader.read[Task])
      tf: ThreadFactory = new ThreadFactoryBuilder()
        .setNameFormat("http-api-thread-pool-%d")
        .setDaemon(false)
        .setPriority(Thread.NORM_PRIORITY)
        .build()
      ec: ExecutionContextExecutor = ExecutionContext.fromExecutor(
        Executors.newFixedThreadPool(settings.httpClientSettings.observerClientThreadsQuantity, tf)
      )
      client <- BlazeClientBuilder[Task](ec).resource
      ht     <- DB[Task](settings)
    } yield (client, ht, settings)

  private def repositories(
    implicit liftIO: LiftConnectionIO[Task]
  ): (HeaderRepository[Task], InputRepository[Task], OutputRepository[Task], TransactionRepository[Task]) =
    (HeaderRepository[Task], InputRepository[Task], OutputRepository[Task], TransactionRepository[Task])
}
