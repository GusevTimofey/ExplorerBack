package encry.explorer.env

import cats.effect.concurrent.Deferred
import cats.effect.{ ConcurrentEffect, ContextShift, Resource }
import cats.syntax.applicative._
import cats.~>
import doobie.free.connection.ConnectionIO
import encry.explorer.core.db.algebra.LiftConnectionIO
import encry.explorer.core.db.algebra.LiftConnectionIO.instances._
import encry.explorer.core.settings.{ ExplorerSettingsContext, SettingsReader }
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import tofu.{ Context, HasContext }

final case class ExplorerContext[F[_]](
  settings: ExplorerSettingsContext,
  logger: SelfAwareStructuredLogger[F],
  sharedQueuesContext: SharedQueuesContext[F],
  initialHeight: Deferred[F, Int]
)

final case class CoreContext[F[_], CI[_]](
  repositoriesContext: RepositoriesContext[CI],
  transactor: CI ~> F
)

final case class ContextContainer[F[_], CI[_]](
  ec: ExplorerContext[F],
  cc: CoreContext[F, CI],
  hc: HttpClientContext[F]
)

object ExplorerContext {

  import cats.effect.Resource._

  private def create[F[_]: ContextShift: ConcurrentEffect, CI[_]: LiftConnectionIO](
    transactor: CI ~> F
  ): Resource[
    F,
    (HasContext[F, CoreContext[F, CI]], HasContext[F, HttpClientContext[F]], HasContext[F, ExplorerContext[F]])
  ] =
    for {
      settings       <- liftF(SettingsReader.read[F])
      logger         <- liftF(Slf4jLogger.create[F])
      sharedQueues   <- liftF(SharedQueuesContext.create[F](settings))
      repositories   <- liftF(RepositoriesContext.create[CI].pure[F])
      clientContext  <- HttpClientContext.create[F](settings.httpClientSettings)
      heightDeferred <- liftF(Deferred[F, Int])
    } yield (
      Context.const(CoreContext[F, CI](repositories, transactor)),
      Context.const(clientContext),
      Context.const(ExplorerContext(settings, logger, sharedQueues, heightDeferred))
    )

  def make[F[_]: ConcurrentEffect: ContextShift] =
    for {
      settings <- liftF(SettingsReader.read[F])
      db       <- DBContext.create[F, ConnectionIO](settings.dbSettings)
      context  <- ExplorerContext.create[F, ConnectionIO](db.transactor.trans)
    } yield context
}
