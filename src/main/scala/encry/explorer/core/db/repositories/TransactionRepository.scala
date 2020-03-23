package encry.explorer.core.db.repositories

import encry.explorer.core.Id
import encry.explorer.core.db.algebra.LiftConnectionIO
import encry.explorer.core.db.algebra.LiftConnectionIO.syntaxConnectionIO._
import encry.explorer.core.db.models.TransactionDBModel
import encry.explorer.core.db.quaries.TransactionsQueries

trait TransactionRepository[F[_]] {

  def getBy(id: Id): F[Option[TransactionDBModel]]

  def getByHeader(id: Id): F[Option[TransactionDBModel]]

  def insert(transaction: TransactionDBModel): F[Int]

}

object TransactionRepository {

  def apply[F[_]: LiftConnectionIO]: TransactionRepository[F] =
    new TransactionRepository[F] {
      override def getBy(id: Id): F[Option[TransactionDBModel]] =
        TransactionsQueries.getBy(id).option.liftF

      override def getByHeader(id: Id): F[Option[TransactionDBModel]] =
        TransactionsQueries.getByHeader(id).option.liftF

      override def insert(transaction: TransactionDBModel): F[Int] =
        TransactionsQueries.insert(transaction).run.liftF
    }
}
