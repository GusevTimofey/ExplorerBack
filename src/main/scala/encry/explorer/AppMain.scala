package encry.explorer

import cats.effect.{ ConcurrentEffect, ContextShift, ExitCode, Timer }
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.{ Monad, Parallel }
import doobie.free.connection.ConnectionIO
import encry.explorer.chain.observer.programs.ObserverProgram
import encry.explorer.core.db.algebra.LiftConnectionIO
import encry.explorer.core.db.algebra.LiftConnectionIO.instances._
import encry.explorer.core.programs.DBProgram
import encry.explorer.env._
import encry.explorer.events.processing.EventsProducer
import monix.eval.{ Task, TaskApp }

object AppMain extends TaskApp {
  override def run(args: List[String]): Task[ExitCode] =
    ExplorerContext.make[Task].use {
      case (cC, clC, ec) =>
        implicit val coreContext     = cC
        implicit val clientContext   = clC
        implicit val explorerContext = ec
        runModules[Task, ConnectionIO].as(ExitCode.Success)
    }

  private def runModules[
    F[_]: Timer: ConcurrentEffect: ContextShift: Parallel,
    CI[_]: LiftConnectionIO: Monad
  ](implicit ec: HasExplorerContext[F], ac: HasHttpApiContext[F], clc: HasCoreContext[F, CI]): F[Unit] = {
    val db: DBProgram[F]      = DBProgram[F, CI]
    val ep: EventsProducer[F] = EventsProducer[F]
    for {
      dbHeight <- db.getBestHeightFromDB
      op       <- ObserverProgram[F, CI](dbHeight)
      _        <- (op.run concurrently db.run concurrently ep.runProducer).compile.drain
    } yield ()
  }
}
