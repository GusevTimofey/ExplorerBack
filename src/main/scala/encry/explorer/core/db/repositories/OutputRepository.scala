package encry.explorer.core.db.repositories

import cats.tagless._
import cats.tagless.implicits._
import doobie.free.connection.ConnectionIO
import encry.explorer.core.db.algebra.LiftConnectionIO
import encry.explorer.core.db.models.OutputDBModel
import encry.explorer.core.db.queries.OutputsQueries
import encry.explorer.core.{ ContractHash, Id }

@autoFunctorK
trait OutputRepository[CI[_]] {

  def getBy(id: Id): CI[Option[OutputDBModel]]

  def getByC(contractHash: ContractHash): CI[Option[OutputDBModel]]

  def getByTransaction(id: Id): CI[Option[OutputDBModel]]

  def insertMany(outputs: List[OutputDBModel]): CI[Int]
}

object OutputRepository {

  private object or extends OutputRepository[ConnectionIO] {
    override def getBy(id: Id): ConnectionIO[Option[OutputDBModel]] =
      OutputsQueries.getBy(id).option

    override def getByC(contractHash: ContractHash): ConnectionIO[Option[OutputDBModel]] =
      OutputsQueries.getByC(contractHash).option

    override def getByTransaction(id: Id): ConnectionIO[Option[OutputDBModel]] =
      OutputsQueries.getByTransaction(id).option

    override def insertMany(outputs: List[OutputDBModel]): ConnectionIO[Int] =
      OutputsQueries.insertMany(outputs)
  }

  def apply[CI[_]](implicit LIO: LiftConnectionIO[CI]): OutputRepository[CI] = or.mapK(LIO.liftConnectionIONT)
}
