package encry.explorer.env

import cats.effect.{ ConcurrentEffect, ContextShift, Resource }
import encry.explorer.core.db.algebra.LiftConnectionIO
import encry.explorer.core.services.SettingsReader
import encry.explorer.core.settings.ExplorerSettings

final case class AppContext[F[_], CI[_]](
  dbContext: DBContext[CI, F],
  httpClientContext: HttpClientContext[F],
  logger: LoggerContext[F],
  explorerSettings: ExplorerSettings,
  sharedQueuesContext: SharedQueuesContext[F],
  httpClientQueuesContext: HttpClientQueuesContext[F]
)

object AppContext {

  def create[F[_]: ConcurrentEffect: ContextShift, CI[_]: LiftConnectionIO]: Resource[F, AppContext[F, CI]] =
    for {
      settings                <- Resource.liftF(SettingsReader.read[F])
      logger                  <- Resource.liftF(LoggerContext.create)
      dbContext               <- DBContext.create[F, CI](settings.dbSettings)
      httpClientContext       <- HttpClientContext.create[F](settings.httpClientSettings)
      sharedQueuesContext     <- Resource.liftF(SharedQueuesContext.create(settings))
      httpClientQueuesContext <- Resource.liftF(HttpClientQueuesContext.create(settings))
    } yield AppContext(dbContext, httpClientContext, logger, settings, sharedQueuesContext, httpClientQueuesContext)

}
