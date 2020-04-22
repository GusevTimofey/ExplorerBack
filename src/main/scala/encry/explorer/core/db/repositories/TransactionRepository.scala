package encry.explorer.core.db.repositories

import encry.explorer.core.Id
import encry.explorer.core.db.algebra.LiftConnectionIO
import encry.explorer.core.db.algebra.LiftConnectionIO.syntaxConnectionIO._
import encry.explorer.core.db.models.TransactionDBModel
import encry.explorer.core.db.queries.TransactionsQueries

trait TransactionRepository[CI[_]] {

  def getBy(id: Id): CI[Option[TransactionDBModel]]

  def getByHeader(id: Id): CI[Option[TransactionDBModel]]

  def insert(transaction: TransactionDBModel): CI[Int]

  def insertMany(transactions: List[TransactionDBModel]): CI[Int]

}

object TransactionRepository {

  def apply[CI[_]: LiftConnectionIO]: TransactionRepository[CI] =
    new TransactionRepository[CI] {
      override def getBy(id: Id): CI[Option[TransactionDBModel]] =
        TransactionsQueries.getBy(id).option.liftEffect

      override def getByHeader(id: Id): CI[Option[TransactionDBModel]] =
        TransactionsQueries.getByHeader(id).option.liftEffect

      override def insert(transaction: TransactionDBModel): CI[Int] =
        TransactionsQueries.insert(transaction).run.liftEffect

      override def insertMany(transactions: List[TransactionDBModel]): CI[Int] =
        TransactionsQueries.insertMany(transactions).liftEffect
    }
}
