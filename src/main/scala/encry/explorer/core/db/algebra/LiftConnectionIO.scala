package encry.explorer.core.db.algebra

import cats.~>
import doobie.free.connection.ConnectionIO
import simulacrum.typeclass

@typeclass trait LiftConnectionIO[F[_]] {

  def liftOp: ConnectionIO ~> F

  def liftF[T](v: ConnectionIO[_]): F[T]
}

object LiftConnectionIO {
  object syntaxConnectionIO {
    implicit class ConnectionIOLiftOps[T](val connectionIO: ConnectionIO[T]) extends AnyVal {
      def liftF[F[_]: LiftConnectionIO]: F[T] = LiftConnectionIO[F].liftF(connectionIO)
    }
  }
}
