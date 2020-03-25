package encry.explorer.core.db.repositories

import encry.explorer.core.db.algebra.LiftConnectionIO
import encry.explorer.core.db.algebra.LiftConnectionIO.syntaxConnectionIO._
import encry.explorer.core.db.models.OutputDBModel
import encry.explorer.core.db.queries.OutputsQueries
import encry.explorer.core.{ ContractHash, Id }

trait OutputRepository[F[_]] {

  def getBy(id: Id): F[Option[OutputDBModel]]

  def getByC(contractHash: ContractHash): F[Option[OutputDBModel]]

  def getByTransaction(id: Id): F[Option[OutputDBModel]]

  def insertMany(outputs: List[OutputDBModel]): F[Int]
}

object OutputRepository {
  def apply[F[_]: LiftConnectionIO]: OutputRepository[F] = new OutputRepository[F] {
    override def getBy(id: Id): F[Option[OutputDBModel]] =
      OutputsQueries.getBy(id).option.liftF

    override def getByC(contractHash: ContractHash): F[Option[OutputDBModel]] =
      OutputsQueries.getByC(contractHash).option.liftF

    override def getByTransaction(id: Id): F[Option[OutputDBModel]] =
      OutputsQueries.getByTransaction(id).option.liftF

    override def insertMany(outputs: List[OutputDBModel]): F[Int] =
      OutputsQueries.insertMany(outputs).liftF
  }
}
