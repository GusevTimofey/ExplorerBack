package encry.explorer.core.services

import cats.{ ~>, Monad }
import cats.syntax.functor._
import encry.explorer.core.Id
import encry.explorer.core.db.algebra.LiftConnectionIO
import encry.explorer.env.ContextDB

trait DBReaderService[F[_]] {

  def getLastIds(quantity: Int): F[List[Id]]

  def getBestHeight: F[Option[Int]]
}

object DBReaderService {
  def apply[F[_]: Monad, CI[_]: LiftConnectionIO](
    transactor: CI ~> F
  )(implicit dBContext: ContextDB[CI, F]): F[DBReaderService[F]] =
    for {
      headerRepo <- dBContext.ask(_.repositoriesContext.hr)
    } yield new DBReaderService[F] {
      override def getLastIds(quantity: Int): F[List[Id]] = transactor(headerRepo.getLast(quantity))

      override def getBestHeight: F[Option[Int]] = transactor(headerRepo.getBestHeight)
    }
}
