package encry.explorer.core.db

import java.util.concurrent.{ Executors, ThreadFactory }

import cats.effect.{ Async, Blocker, ContextShift, Resource, Sync }
import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.zaxxer.hikari.HikariDataSource
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import encry.explorer.core.settings.DBSettings

import scala.concurrent.ExecutionContext

object DB {

  def apply[F[_]: Async: ContextShift](SR: DBSettings): Resource[F, HikariTransactor[F]] =
    for {
      ec <- ExecutionContexts.fixedThreadPool[F](SR.connectionsPoolSize)
      tf: ThreadFactory = new ThreadFactoryBuilder()
        .setNameFormat("blocker-doobie-%d")
        .setDaemon(false)
        .setPriority(Thread.NORM_PRIORITY)
        .build()
      ecb = ExecutionContext.fromExecutor(Executors.newCachedThreadPool(tf))
      bc  = Blocker.liftExecutionContext(ecb)
      xt <- HikariTransactor.newHikariTransactor[F](
             SR.jdbcDriver,
             SR.dbUrl,
             SR.login,
             SR.password,
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
              ds setPoolName SR.poolName
              ds setMaximumPoolSize SR.hikaryPoolSize
            }
          })
    } yield xt
}
