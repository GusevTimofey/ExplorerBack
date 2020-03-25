package encry.explorer.core.services

import cats.effect.Sync
import cats.syntax.functor._
import encry.explorer.core.settings.ExplorerSettings
import pureconfig.ConfigSource
import pureconfig.module.catseffect.syntax._
import pureconfig._
import pureconfig.generic.auto._

trait SettingsReader[F[_]] {
  val settings: ExplorerSettings
}

object SettingsReader {
  def apply[F[_]: Sync]: F[SettingsReader[F]] =
    for {
      configs <- ConfigSource.default.withFallback(ConfigSource.file("local.conf")).loadF[F, ExplorerSettings]
    } yield new SettingsReader[F] { override val settings: ExplorerSettings = configs }
}
