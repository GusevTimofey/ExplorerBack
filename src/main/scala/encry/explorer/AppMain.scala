package encry.explorer

import cats.effect.{ ExitCode, IO, IOApp, Resource }
import cats.{ ~>, Applicative }
import cats.syntax.functor._
import cats.syntax.applicative._
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
import fs2.concurrent.Queue
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder

import scala.concurrent.ExecutionContext

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
          queue <- Queue.bounded[IO, HttpApiBlock](100)
          no    <- NetworkObserver.apply[IO](client, queue, sr).pure[IO]
          db <- DBService
                 .apply[IO](
                   queue,
                   HeaderRepository.apply[IO],
                   InputRepository.apply[IO],
                   OutputRepository.apply[IO],
                   TransactionRepository.apply[IO]
                 )
                 .pure[IO]
          _ <- (no.run concurrently db.run).compile.drain
        } yield ()).as(ExitCode.Success)
    }

  def resources: Resource[IO, (Client[IO], HikariTransactor[IO], SettingsReader[IO])] =
    for {
      client   <- BlazeClientBuilder[IO](ExecutionContext.global).resource
      settings <- Resource.liftF(SettingsReader.apply[IO])
      ht       <- DB.apply(settings)
    } yield (client, ht, settings)
}
