package encry.explorer

import java.util.concurrent.{ Executors, ThreadFactory }

import cats.Parallel
import cats.effect.{ ConcurrentEffect, ContextShift, ExitCode, Resource, Timer }
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.google.common.util.concurrent.ThreadFactoryBuilder
import doobie.free.connection.ConnectionIO
import doobie.hikari.HikariTransactor
import encry.explorer.chain.observer.http.api.models.HttpApiBlock
import encry.explorer.chain.observer.programs.ObserverProgram
import encry.explorer.core.db.DB
import encry.explorer.core.db.algebra.LiftConnectionIO
import encry.explorer.core.db.algebra.LiftConnectionIO.instances._
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
  override def run(args: List[String]): Task[ExitCode] = runProgram[Task]

  private def runProgram[F[_]: ConcurrentEffect: ContextShift: Timer: Parallel] =
    resources[F].use {
      case (client, ht, sr) =>
        (for {
          implicit0(logger: SelfAwareStructuredLogger[F]) <- Slf4jLogger.create[F]

          _                <- logger.info(s"Resources and implicit values were initialised successfully.")
          bestChainBlocks  <- Queue.bounded[F, HttpApiBlock](sr.encrySettings.rollbackMaxHeight * 2)
          forkBlocks       <- Queue.bounded[F, String](sr.encrySettings.rollbackMaxHeight)
          (ir, or, tr) = repositories[F, ConnectionIO]
          _                <- logger.info(s"All repositories were initialised successfully.")
          hr11             = HeaderRepository[ConnectionIO](liftConnectionIOInstance.liftConnectionIONT)
          dbReader         = DBReaderService[F, ConnectionIO](hr11, ht.trans)
          _                <- logger.info(s"DB reader was initialised successfully.")
          db               = DBService[F, ConnectionIO](bestChainBlocks, forkBlocks, hr11, ir, or, tr, ht.trans)
          _                <- logger.info(s"DB service was created successfully.")
          dbHeight         <- db.getBestHeightFromDB
          _                <- logger.info(s"Last height in the explorer DB is: $dbHeight.")
          eventsQueue      <- Queue.unbounded[F, ExplorerEvent]
          ep               = EventsProducer[F](eventsQueue, sr)
          op               <- ObserverProgram[F](client, dbReader, forkBlocks, bestChainBlocks, eventsQueue, dbHeight, sr)
          _                <- logger.info(s"Chain observer program stated successfully.")
          _                <- (op.run concurrently db.run concurrently ep.runProducer).compile.drain
          _                <- logger.info(s"Explorer app has been started. Last height in the data base is: $dbHeight.")
        } yield ()).as(ExitCode.Success)
    }

  private def resources[F[_]: ConcurrentEffect: ContextShift]
    : Resource[F, (Client[F], HikariTransactor[F], ExplorerSettings)] =
    for {
      settings <- Resource.liftF(SettingsReader.read[F])
      tf: ThreadFactory = new ThreadFactoryBuilder()
        .setNameFormat("http-api-thread-pool-%d")
        .setDaemon(false)
        .setPriority(Thread.NORM_PRIORITY)
        .build()
      ec: ExecutionContextExecutor = ExecutionContext.fromExecutor(
        Executors.newFixedThreadPool(settings.httpClientSettings.observerClientThreadsQuantity, tf)
      )
      client <- BlazeClientBuilder[F](ec).resource
      ht     <- DB[F](settings)
    } yield (client, ht, settings)

  private def repositories[F[_], CI[_]: LiftConnectionIO] =
    (InputRepository[CI], OutputRepository[CI], TransactionRepository[CI])
}
