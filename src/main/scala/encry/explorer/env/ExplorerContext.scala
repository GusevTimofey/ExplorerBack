package encry.explorer.env

import cats.effect.{ Concurrent, ConcurrentEffect, ContextShift, Resource, Sync }
import cats.syntax.applicative._
import cats.~>
import doobie.free.connection.ConnectionIO
import encry.explorer.core.db.algebra.LiftConnectionIO
import encry.explorer.core.settings.{ ExplorerSettingsContext, SettingsReader }
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import monix.eval.Task
import tofu.{ Context, HasContext }

final case class ExplorerContext[F[_]](
  settings: ExplorerSettingsContext,
  logger: SelfAwareStructuredLogger[F],
  sharedQueuesContext: SharedQueuesContext[F]
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

  private def create[F[_]: ConcurrentEffect: ContextShift, CI[_]: LiftConnectionIO](
    transactor: CI ~> F,
    settings: ExplorerSettingsContext
  ): Resource[F, ContextContainer[F, CI]] =
    for {
      logger        <- liftF(Slf4jLogger.create[F])
      sharedQueues  <- liftF(SharedQueuesContext.create[F](settings))
      repositories  <- liftF(RepositoriesContext.create[CI].pure[F])
      clientContext <- HttpClientContext.create[F](settings.httpClientSettings)
    } yield ContextContainer[F, CI](
      ExplorerContext(settings, logger, sharedQueues),
      CoreContext[F, CI](repositories, transactor),
      clientContext
    )

  def make[F[_]: ContextShift: ConcurrentEffect, CI[_]: LiftConnectionIO](
    transact: CI ~> F
  ): Resource[
    F,
    (HasContext[F, CoreContext[F, CI]], HasContext[F, HttpClientContext[F]], HasContext[F, ExplorerContext[F]])
  ] =
    for {
      settings         <- liftF(SettingsReader.read[F])
      contextContainer <- ExplorerContext.create[F, CI](transact, settings)
    } yield (
      Context.const(contextContainer.cc),
      Context.const(contextContainer.hc),
      Context.const(contextContainer.ec)
    )
}
