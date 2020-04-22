package encry.explorer.core.db.repositories

import encry.explorer.core.db.algebra.LiftConnectionIO
import encry.explorer.core.db.algebra.LiftConnectionIO.syntaxConnectionIO._
import encry.explorer.core.db.models.OutputDBModel
import encry.explorer.core.db.queries.OutputsQueries
import encry.explorer.core.{ ContractHash, Id }

trait OutputRepository[CI[_]] {

  def getBy(id: Id): CI[Option[OutputDBModel]]

  def getByC(contractHash: ContractHash): CI[Option[OutputDBModel]]

  def getByTransaction(id: Id): CI[Option[OutputDBModel]]

  def insertMany(outputs: List[OutputDBModel]): CI[Int]
}

object OutputRepository {
  def apply[CI[_]: LiftConnectionIO]: OutputRepository[CI] = new OutputRepository[CI] {
    override def getBy(id: Id): CI[Option[OutputDBModel]] =
      OutputsQueries.getBy(id).option.liftEffect

    override def getByC(contractHash: ContractHash): CI[Option[OutputDBModel]] =
      OutputsQueries.getByC(contractHash).option.liftEffect

    override def getByTransaction(id: Id): CI[Option[OutputDBModel]] =
      OutputsQueries.getByTransaction(id).option.liftEffect

    override def insertMany(outputs: List[OutputDBModel]): CI[Int] =
      OutputsQueries.insertMany(outputs).liftEffect
  }
}
