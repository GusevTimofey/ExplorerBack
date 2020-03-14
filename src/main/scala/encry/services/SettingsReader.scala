package encry.services

import cats.Applicative
import cats.syntax.applicative._
import cats.syntax.functor._
import encry.settings.ExplorerSettings
import pureconfig.ConfigSource

trait SettingsReader[F[_]] {
  val settings: ExplorerSettings
}

object SettingsReader {
  def apply[F[_]: Applicative]: F[SettingsReader[F]] =
    for {
      configs <- ConfigSource.default.loadOrThrow[ExplorerSettings].pure[F]
    } yield new SettingsReader[F] { override val settings: ExplorerSettings = configs }
}
