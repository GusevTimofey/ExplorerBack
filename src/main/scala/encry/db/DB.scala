package encry.db

import cats.Applicative
import cats.effect.{ Async, Blocker, ContextShift, Resource }
import com.zaxxer.hikari.HikariDataSource
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import encry.services.SettingsReader

object DB {

  def apply[F[_]: Async: ContextShift](implicit SS: SettingsReader[F]): Resource[F, HikariTransactor[F]] =
    for {
      ec <- ExecutionContexts.fixedThreadPool[F](SS.settings.dbSettings.connectionsPoolSize)
      bc <- Blocker[F]
      xt <- HikariTransactor.newHikariTransactor[F](
             SS.settings.dbSettings.jdbcDriver,
             SS.settings.dbSettings.dbUrl,
             SS.settings.dbSettings.login,
             SS.settings.dbSettings.password,
             ec,
             bc
           )
      _ <- Resource.liftF(xt.configure((_: HikariDataSource) => Applicative[F].unit))
    } yield xt
}
