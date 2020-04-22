package encry.explorer.core.db.algebra

import doobie.free.connection.ConnectionIO
import simulacrum.typeclass

@typeclass trait LiftConnectionIO[F[_]] {

  def liftEffect[T](v: ConnectionIO[T]): F[T]
}

object LiftConnectionIO {
  object syntaxConnectionIO {
    implicit class ConnectionIOLiftOps[T](val connectionIO: ConnectionIO[T]) extends AnyVal {
      def liftEffect[CI[_]: LiftConnectionIO]: CI[T] = LiftConnectionIO[CI].liftEffect(connectionIO)
    }
  }

  object instances {
    implicit val liftConnectionIOInstance: LiftConnectionIO[ConnectionIO] =
      new LiftConnectionIO[ConnectionIO] {
        override def liftEffect[T](v: ConnectionIO[T]): ConnectionIO[T] = v
      }
  }
}
