package encry.explorer

import cats.effect.{ ExitCode, IO, IOApp, Resource }
import cats.~>
import cats.syntax.functor._
import cats.syntax.applicative._
import doobie.free.connection.ConnectionIO
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
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.http4s.client.blaze.BlazeClientBuilder

import scala.concurrent.ExecutionContext

object AppMain extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    BlazeClientBuilder[IO](ExecutionContext.global).resource.use { client =>
      Slf4jLogger
        .create[IO]
        .map { implicit logging =>
          for {
            queue    <- Resource.liftF(Queue.bounded[IO, HttpApiBlock](100))
            settings <- Resource.liftF(SettingsReader.apply[IO])
            ht       <- DB.apply(settings)
            implicit0(liftIO: LiftConnectionIO[IO]) <- Resource.liftF(new LiftConnectionIO[IO] {
                                                        override def liftOp: ConnectionIO ~> IO = ht.trans
                                                        override def liftF[T](v: ConnectionIO[T]): IO[T] =
                                                          liftOp.apply(v)
                                                      }.pure[IO])
            no <- Resource.liftF(NetworkObserver.apply[IO](client, queue).pure[IO])
            db <- Resource.liftF(
                   DBService
                     .apply[IO](
                       queue,
                       HeaderRepository.apply[IO],
                       InputRepository.apply[IO],
                       OutputRepository.apply[IO],
                       TransactionRepository.apply[IO]
                     )
                     .pure[IO]
                 )
          } yield no.run concurrently db.run
        }
        .as(ExitCode.Success)

    }
}
