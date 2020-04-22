package encry.explorer.core.services

import cats.~>
import encry.explorer.core.Id
import encry.explorer.core.db.algebra.LiftConnectionIO
import encry.explorer.core.db.repositories.HeaderRepository

trait DBReaderService[F[_]] {

  def getLastIds(quantity: Int): F[List[Id]]

  def getBestHeight: F[Option[Int]]
}

object DBReaderService {
  def apply[F[_], CI[_]: LiftConnectionIO](
    headerRepository: HeaderRepository[CI],
    transformF: CI ~> F
  ): DBReaderService[F] = new DBReaderService[F] {
    override def getLastIds(quantity: Int): F[List[Id]] = transformF(headerRepository.getLast(quantity))

    override def getBestHeight: F[Option[Int]] = transformF(headerRepository.getBestHeight)
  }
}
