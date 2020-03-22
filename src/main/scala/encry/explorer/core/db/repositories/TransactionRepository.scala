package encry.explorer.core.db.repositories

import encry.explorer.core.Id
import encry.explorer.core.db.algebra.LiftConnectionIO
import encry.explorer.core.db.algebra.LiftConnectionIO.syntaxConnectionIO._
import encry.explorer.core.db.models.Transaction
import encry.explorer.core.db.quaries.TransactionsQueries

trait TransactionRepository[F[_]] {

  def getBy(id: Id): F[Option[Transaction]]

  def getByHeader(id: Id): F[Option[Transaction]]

  def insert(transaction: Transaction): F[Int]

}

object TransactionRepository {

  def apply[F[_]: LiftConnectionIO]: TransactionRepository[F] =
    new TransactionRepository[F] {
      override def getBy(id: Id): F[Option[Transaction]] =
        TransactionsQueries.getBy(id).option.liftF

      override def getByHeader(id: Id): F[Option[Transaction]] =
        TransactionsQueries.getByHeader(id).option.liftF

      override def insert(transaction: Transaction): F[Int] =
        TransactionsQueries.insert(transaction).run.liftF
    }
}
