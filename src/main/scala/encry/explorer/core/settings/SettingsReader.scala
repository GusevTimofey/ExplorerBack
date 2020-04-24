package encry.explorer.core.settings

import cats.effect.Sync
import pureconfig.ConfigSource
import pureconfig.module.catseffect.syntax._
import pureconfig._
import pureconfig.generic.auto._

object SettingsReader {
  def read[F[_]: Sync]: F[ExplorerSettingsContext] =
    ConfigSource
      .file(s"${System.getProperty("user.dir")}/src/main/resources/local.conf")
      .withFallback(ConfigSource.default)
      .loadF[F, ExplorerSettingsContext]
}
