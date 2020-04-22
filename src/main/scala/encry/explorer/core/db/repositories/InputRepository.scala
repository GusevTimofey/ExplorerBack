package encry.explorer.core.db.repositories

import encry.explorer.core.Id
import encry.explorer.core.db.algebra.LiftConnectionIO
import encry.explorer.core.db.algebra.LiftConnectionIO.syntaxConnectionIO._
import encry.explorer.core.db.models.InputDBModel
import encry.explorer.core.db.queries.InputsQueries

trait InputRepository[CI[_]] {

  def getBy(id: Id): CI[Option[InputDBModel]]

  def getByTransaction(id: Id): CI[Option[InputDBModel]]

  def insertMany(inputs: List[InputDBModel]): CI[Int]

  def updateIsActiveField(id: Id, isActive: Boolean): CI[Int]

}

object InputRepository {
  def apply[CI[_]: LiftConnectionIO]: InputRepository[CI] = new InputRepository[CI] {
    override def getBy(id: Id): CI[Option[InputDBModel]] =
      InputsQueries.getBy(id).option.liftEffect

    override def getByTransaction(id: Id): CI[Option[InputDBModel]] =
      InputsQueries.getByTransaction(id).option.liftEffect

    override def insertMany(inputs: List[InputDBModel]): CI[Int] =
      InputsQueries.insertMany(inputs).liftEffect

    override def updateIsActiveField(id: Id, isActive: Boolean): CI[Int] =
      InputsQueries.updateIsActiveField(id, isActive).run.liftEffect
  }
}
