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
      configs <- ConfigSource
                  .file(s"${System.getProperty("user.dir")}/src/main/resources/local.conf")
                  .withFallback(ConfigSource.default)
                  .loadF[F, ExplorerSettings]
    } yield {
      println(configs)
      new SettingsReader[F] { override val settings: ExplorerSettings = configs }
    }
}
