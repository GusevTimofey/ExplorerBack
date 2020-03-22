package encry.explorer.core.db.repositories

import encry.explorer.core.Id
import encry.explorer.core.db.algebra.LiftConnectionIO
import encry.explorer.core.db.algebra.LiftConnectionIO.syntaxConnectionIO._
import encry.explorer.core.db.models.Input
import encry.explorer.core.db.quaries.InputsQueries

trait InputRepository[F[_]] {

  def getBy(id: Id): F[Option[Input]]

  def getByTransaction(id: Id): F[Option[Input]]

  def insertMany(inputs: List[Input]): F[Int]

  def updateIsActiveField(id: Id, isActive: Boolean): F[Int]

}

object InputRepository {
  def apply[F[_]: LiftConnectionIO]: InputRepository[F] = new InputRepository[F] {
    override def getBy(id: Id): F[Option[Input]] =
      InputsQueries.getBy(id).option.liftF

    override def getByTransaction(id: Id): F[Option[Input]] =
      InputsQueries.getByTransaction(id).option.liftF

    override def insertMany(inputs: List[Input]): F[Int] =
      InputsQueries.insertMany(inputs).liftF

    override def updateIsActiveField(id: Id, isActive: Boolean): F[Int] =
      InputsQueries.updateIsActiveField(id, isActive).run.liftF
  }
}
