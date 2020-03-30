package encry.explorer.core.db.repositories

import encry.explorer.core._
import encry.explorer.core.db.algebra.LiftConnectionIO
import encry.explorer.core.db.algebra.LiftConnectionIO.syntaxConnectionIO._
import encry.explorer.core.db.models.HeaderDBModel
import encry.explorer.core.db.queries.HeadersQueries

trait HeaderRepository[F[_]] {

  def getBy(id: Id): F[Option[HeaderDBModel]]

  def getByH(height: HeaderHeight): F[Option[HeaderDBModel]]

  def getByParent(id: Id): F[Option[HeaderDBModel]]

  def getBestAt(height: HeaderHeight): F[Option[HeaderDBModel]]

  def insert(header: HeaderDBModel): F[Int]

  def updateBestChainField(id: Id, statement: Boolean): F[Int]

  def getBestHeight: F[Option[Int]]
}

object HeaderRepository {

  def apply[F[_]: LiftConnectionIO]: HeaderRepository[F] =
    new HeaderRepository[F] {
      override def getBy(id: Id): F[Option[HeaderDBModel]] =
        HeadersQueries.getBy(id).option.liftF

      override def getByH(height: HeaderHeight): F[Option[HeaderDBModel]] =
        HeadersQueries.getByH(height).option.liftF

      override def getByParent(id: Id): F[Option[HeaderDBModel]] =
        HeadersQueries.getByParent(id).option.liftF

      override def getBestAt(height: HeaderHeight): F[Option[HeaderDBModel]] =
        HeadersQueries.getBestAt(height).option.liftF

      override def insert(header: HeaderDBModel): F[Int] =
        HeadersQueries.insert(header).liftF

      override def updateBestChainField(id: Id, statement: Boolean): F[Int] =
        HeadersQueries.updateBestChainField(id, statement).run.liftF

      override def getBestHeight: F[Option[Int]] =
        HeadersQueries.getBestHeight.option.liftF
    }
}
