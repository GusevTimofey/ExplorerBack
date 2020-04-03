package encry.explorer.core.services

import encry.explorer.core.Id
import encry.explorer.core.db.repositories.HeaderRepository

trait DBReaderService[F[_]] {

  def getLastIds(quantity: Int): F[List[Id]]

  def getBestHeight: F[Option[Int]]
}

object DBReaderService {
  def apply[F[_]](headerRepository: HeaderRepository[F]): DBReaderService[F] = new DBReaderService[F] {
    override def getLastIds(quantity: Int): F[List[Id]] = headerRepository.getLast(quantity)

    override def getBestHeight: F[Option[Int]] = headerRepository.getBestHeight
  }
}
