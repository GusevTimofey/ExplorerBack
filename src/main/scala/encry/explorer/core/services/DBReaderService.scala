package encry.explorer.core.services

import cats.Monad
import encry.explorer.core.Id
import encry.explorer.core.db.algebra.LiftConnectionIO
import encry.explorer.env.HasCoreContext

trait DBReaderService[F[_]] {

  def getLastIds(quantity: Int): F[List[Id]]

  def getBestHeight: F[Option[Int]]
}

object DBReaderService {
  def apply[F[_]: Monad, CI[_]: LiftConnectionIO](
    implicit coreContext: HasCoreContext[F, CI]
  ): DBReaderService[F] =
    new DBReaderService[F] {
      override def getLastIds(quantity: Int): F[List[Id]] =
        coreContext.askF(cxt => cxt.transactor(cxt.repositoriesContext.hr.getLast(quantity)))

      override def getBestHeight: F[Option[Int]] =
        coreContext.askF(cxt => cxt.transactor(cxt.repositoriesContext.hr.getBestHeight))
    }
}
