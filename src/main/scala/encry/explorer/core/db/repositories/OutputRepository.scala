package encry.explorer.core.db.repositories

import encry.explorer.core.db.algebra.LiftConnectionIO
import encry.explorer.core.db.algebra.LiftConnectionIO.syntaxConnectionIO._
import encry.explorer.core.db.models.Output
import encry.explorer.core.db.quaries.OutputsQueries
import encry.explorer.core.{ ContractHash, Id }

trait OutputRepository[F[_]] {

  def getBy(id: Id): F[Option[Output]]

  def getBy(contractHash: ContractHash): F[Option[Output]]

  def getByTransaction(id: Id): F[Option[Output]]

  def insertMany(outputs: List[Output]): F[Int]
}

object OutputRepository {
  def apply[F[_]: LiftConnectionIO]: OutputRepository[F] = new OutputRepository[F] {
    override def getBy(id: Id): F[Option[Output]] =
      OutputsQueries.getBy(id).option.liftF

    override def getBy(contractHash: ContractHash): F[Option[Output]] =
      OutputsQueries.getBy(contractHash).option.liftF

    override def getByTransaction(id: Id): F[Option[Output]] =
      OutputsQueries.getByTransaction(id).option.liftF

    override def insertMany(outputs: List[Output]): F[Int] =
      OutputsQueries.insertMany(outputs).liftF
  }
}
