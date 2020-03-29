package encry.explorer.core.db

import cats.effect.{ Async, Blocker, ContextShift, Resource, Sync }
import com.zaxxer.hikari.HikariDataSource
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import encry.explorer.core.settings.ExplorerSettings

object DB {

  def apply[F[_]: Async: ContextShift](SR: ExplorerSettings): Resource[F, HikariTransactor[F]] =
    for {
      ec <- ExecutionContexts.fixedThreadPool[F](SR.dbSettings.connectionsPoolSize)
      bc <- Blocker[F]
      xt <- HikariTransactor.newHikariTransactor[F](
             SR.dbSettings.jdbcDriver,
             SR.dbSettings.dbUrl,
             SR.dbSettings.login,
             SR.dbSettings.password,
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
              ds setPoolName SR.dbSettings.poolName
              ds setMaximumPoolSize SR.dbSettings.connectionsPoolSize
            }
          })
    } yield xt
}
