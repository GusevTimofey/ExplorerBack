package encry.explorer.core.db.repositories

import cats.tagless._
import cats.tagless.implicits._
import doobie.free.connection.ConnectionIO
import encry.explorer.core.Id
import encry.explorer.core.db.algebra.LiftConnectionIO
import encry.explorer.core.db.models.InputDBModel
import encry.explorer.core.db.queries.InputsQueries

@autoFunctorK
trait InputRepository[CI[_]] {

  def getBy(id: Id): CI[Option[InputDBModel]]

  def getByTransaction(id: Id): CI[Option[InputDBModel]]

  def insertMany(inputs: List[InputDBModel]): CI[Int]

  def updateIsActiveField(id: Id, isActive: Boolean): CI[Int]

}

object InputRepository {

  private object ir extends InputRepository[ConnectionIO] {
    override def getBy(id: Id): ConnectionIO[Option[InputDBModel]] =
      InputsQueries.getBy(id).option

    override def getByTransaction(id: Id): ConnectionIO[Option[InputDBModel]] =
      InputsQueries.getByTransaction(id).option

    override def insertMany(inputs: List[InputDBModel]): ConnectionIO[Int] =
      InputsQueries.insertMany(inputs)

    override def updateIsActiveField(id: Id, isActive: Boolean): ConnectionIO[Int] =
      InputsQueries.updateIsActiveField(id, isActive).run
  }
  def apply[CI[_]](implicit LIO: LiftConnectionIO[CI]): InputRepository[CI] = ir.mapK(LIO.liftConnectionIONT)
}
