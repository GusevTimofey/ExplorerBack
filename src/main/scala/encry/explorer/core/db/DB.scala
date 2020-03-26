package encry.explorer.core.db

import cats.effect.{ Async, Blocker, ContextShift, Resource, Sync }
import com.zaxxer.hikari.HikariDataSource
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import encry.explorer.core.services.SettingsReader

import scala.concurrent.duration._

object DB {

  def apply[F[_]: Async: ContextShift](SR: SettingsReader[F]): Resource[F, HikariTransactor[F]] =
    for {
      ec <- ExecutionContexts.fixedThreadPool[F](SR.settings.dbSettings.connectionsPoolSize)
      bc <- Blocker[F]
      xt <- HikariTransactor.newHikariTransactor[F](
             SR.settings.dbSettings.jdbcDriver,
             SR.settings.dbSettings.dbUrl,
             SR.settings.dbSettings.login,
             SR.settings.dbSettings.password,
             ec,
             bc
           )
      _ <- Resource.liftF(xt.configure { ds: HikariDataSource =>
            Sync[F].delay {
              ds setAutoCommit false
              ds setConnectionTimeout 120000
              ds setMinimumIdle 2
              ds setIdleTimeout 300000
              ds setMaxLifetime 600000
              ds setPoolName SR.settings.dbSettings.poolName
              ds setMaximumPoolSize SR.settings.dbSettings.connectionsPoolSize
            }
          })
    } yield xt
}
