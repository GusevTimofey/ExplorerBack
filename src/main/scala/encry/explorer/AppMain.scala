package encry.explorer

import cats.effect.{ ExitCode, IO, IOApp }
import cats.syntax.functor._
import encry.explorer.chain.observer.programs.ChainSynchronizer
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.http4s.client.blaze.BlazeClientBuilder

import scala.concurrent.ExecutionContext

object AppMain extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    BlazeClientBuilder[IO](ExecutionContext.global).resource.use { client =>
      Slf4jLogger
        .create[IO]
        .flatMap { implicit logging =>
          ChainSynchronizer
            .apply[IO](client)
            .flatMap(_.run.compile.drain)
        }
        .as(ExitCode.Success)
    }

}
