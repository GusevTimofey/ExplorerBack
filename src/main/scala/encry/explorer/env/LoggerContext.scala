package encry.explorer.env

import cats.effect.Sync
import cats.syntax.functor._
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger

case class LoggerContext[F[_]](logger: Logger[F])

object LoggerContext {
  def create[F[_]: Sync]: F[LoggerContext[F]] = Slf4jLogger.create[F].map(LoggerContext(_))
}
