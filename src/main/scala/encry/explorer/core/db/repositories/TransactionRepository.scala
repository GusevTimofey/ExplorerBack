package encry.explorer.core.db.repositories

import cats.tagless._
import cats.tagless.implicits._
import doobie.free.connection.ConnectionIO
import encry.explorer.core.Id
import encry.explorer.core.db.algebra.LiftConnectionIO
import encry.explorer.core.db.models.TransactionDBModel
import encry.explorer.core.db.queries.TransactionsQueries

@autoFunctorK
trait TransactionRepository[CI[_]] {

  def getBy(id: Id): CI[Option[TransactionDBModel]]

  def getByHeader(id: Id): CI[Option[TransactionDBModel]]

  def insert(transaction: TransactionDBModel): CI[Int]

  def insertMany(transactions: List[TransactionDBModel]): CI[Int]

}

object TransactionRepository {

  private object tr extends TransactionRepository[ConnectionIO] {
    override def getBy(id: Id): ConnectionIO[Option[TransactionDBModel]] =
      TransactionsQueries.getBy(id).option

    override def getByHeader(id: Id): ConnectionIO[Option[TransactionDBModel]] =
      TransactionsQueries.getByHeader(id).option

    override def insert(transaction: TransactionDBModel): ConnectionIO[Int] =
      TransactionsQueries.insert(transaction).run

    override def insertMany(transactions: List[TransactionDBModel]): ConnectionIO[Int] =
      TransactionsQueries.insertMany(transactions)
  }

  def apply[CI[_]](implicit LIO: LiftConnectionIO[CI]): TransactionRepository[CI] = tr.mapK(LIO.liftConnectionIONT)
}
