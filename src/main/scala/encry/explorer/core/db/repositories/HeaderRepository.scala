package encry.explorer.core.db.repositories

import encry.explorer.core._
import encry.explorer.core.db.algebra.LiftConnectionIO
import encry.explorer.core.db.algebra.LiftConnectionIO.syntaxConnectionIO._
import encry.explorer.core.db.models.Header
import encry.explorer.core.db.quaries.HeadersQueries

trait HeaderRepository[F[_]] {

  def getBy(id: Id): F[Option[Header]]

  def getBy(height: HeaderHeight): F[Option[Header]]

  def getByParent(id: Id): F[Option[Header]]

  def getBestAt(height: HeaderHeight): F[Option[Header]]

  def insert(header: Header): F[Int]

  def updateBestChainField(id: Id, statement: Boolean): F[Int]
}

object HeaderRepository {

  def apply[F[_]: LiftConnectionIO]: HeaderRepository[F] =
    new HeaderRepository[F] {
      override def getBy(id: Id): F[Option[Header]] =
        HeadersQueries.getBy(id).option.liftF

      override def getBy(height: HeaderHeight): F[Option[Header]] =
        HeadersQueries.getBy(height).option.liftF

      override def getByParent(id: Id): F[Option[Header]] =
        HeadersQueries.getByParent(id).option.liftF

      override def getBestAt(height: HeaderHeight): F[Option[Header]] =
        HeadersQueries.getBestAt(height).option.liftF

      override def insert(header: Header): F[Int] =
        HeadersQueries.insert(header).run.liftF

      override def updateBestChainField(id: Id, statement: Boolean): F[Int] =
        HeadersQueries.updateBestChainField(id, statement).run.liftF
    }
}
