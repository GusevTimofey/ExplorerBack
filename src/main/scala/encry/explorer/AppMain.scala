package encry.explorer

import cats.Parallel
import cats.effect.{ ConcurrentEffect, ContextShift, ExitCode, Timer }
import cats.free.Free.catsFreeMonadForFree
import cats.syntax.flatMap._
import cats.syntax.functor._
import doobie.free.connection.ConnectionIO
import encry.explorer.chain.observer.programs.ObserverProgram
import encry.explorer.core.db.algebra.LiftConnectionIO.instances._
import encry.explorer.core.services.{ DBReaderService, DBService }
import encry.explorer.env._
import encry.explorer.events.processing.EventsProducer
import monix.eval.{ Task, TaskApp }
import tofu.{ Context, HasContext }

object AppMain extends TaskApp {
  override def run(args: List[String]): Task[ExitCode] = runExplorer[Task]

  private def runExplorer[F[_]: ConcurrentEffect: ContextShift: Parallel: Timer]: F[ExitCode] =
    AppContext.create[F, ConnectionIO].use { cxt =>
      implicit val context: HasContext[F, AppContext[F, ConnectionIO]] =
        Context.const[F, AppContext[F, ConnectionIO]](cxt)
      runPrograms[F].as(ExitCode.Success)
    }

  private def runPrograms[F[_]: ConcurrentEffect: Timer: ContextShift: Parallel](
    implicit contextApplication: ContextApplication[F, ConnectionIO]
  ): F[Unit] =
    for {
      sr <- contextApplication.ask(_.explorerSettings)
      implicit0(dbC: HasContext[F, DBContext[ConnectionIO, F]]) <- contextApplication
                                                                    .ask(_.dbContext)
                                                                    .map(
                                                                      Context.const[F, DBContext[ConnectionIO, F]](_)
                                                                    )
      implicit0(sharedQC: HasContext[F, SharedQueuesContext[F]]) <- contextApplication
                                                                     .ask(_.sharedQueuesContext)
                                                                     .map(Context.const[F, SharedQueuesContext[F]](_))
      implicit0(loggingC: HasContext[F, LoggerContext[F]]) <- contextApplication
                                                               .ask(_.logger)
                                                               .map(Context.const[F, LoggerContext[F]](_))
      implicit0(httpClientC: HasContext[F, HttpClientContext[F]]) <- contextApplication
                                                                      .ask(_.httpClientContext)
                                                                      .map(Context.const[F, HttpClientContext[F]](_))
      implicit0(observerQC: HasContext[F, HttpClientQueuesContext[F]]) <- contextApplication
                                                                           .ask(_.httpClientQueuesContext)
                                                                           .map(
                                                                             Context
                                                                               .const[F, HttpClientQueuesContext[F]](_)
                                                                           )
      dbReader <- contextApplication.askF(l => DBReaderService[F, ConnectionIO](l.dbContext.transactor.trans))
      db       <- contextApplication.askF(l => DBService[F, ConnectionIO](l.dbContext.transactor.trans))
      dbHeight <- db.getBestHeightFromDB
      ep       <- EventsProducer[F](sr)
      op       <- ObserverProgram[F](dbReader, dbHeight, sr)
      _        <- (op.run concurrently db.run concurrently ep.runProducer).compile.drain
    } yield ()
}
