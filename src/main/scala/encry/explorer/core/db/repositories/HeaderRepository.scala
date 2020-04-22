package encry.explorer.core.db.repositories

import encry.explorer.core._
import encry.explorer.core.db.algebra.LiftConnectionIO
import encry.explorer.core.db.algebra.LiftConnectionIO.syntaxConnectionIO._
import encry.explorer.core.db.models.HeaderDBModel
import encry.explorer.core.db.queries.HeadersQueries

trait HeaderRepository[CI[_]] {

  def getBy(id: Id): CI[Option[HeaderDBModel]]

  def getByH(height: HeaderHeight): CI[Option[HeaderDBModel]]

  def getByParent(id: Id): CI[Option[HeaderDBModel]]

  def getBestAt(height: HeaderHeight): CI[Option[HeaderDBModel]]

  def insert(header: HeaderDBModel): CI[Int]

  def updateBestChainField(id: Id, statement: Boolean): CI[Int]

  def getBestHeight: CI[Option[Int]]

  def getLast(quantity: Int): CI[List[Id]]
}

object HeaderRepository {

  def apply[CI[_]: LiftConnectionIO]: HeaderRepository[CI] =
    new HeaderRepository[CI] {
      override def getBy(id: Id): CI[Option[HeaderDBModel]] =
        HeadersQueries.getBy(id).option.liftEffect

      override def getByH(height: HeaderHeight): CI[Option[HeaderDBModel]] =
        HeadersQueries.getByH(height).option.liftEffect

      override def getByParent(id: Id): CI[Option[HeaderDBModel]] =
        HeadersQueries.getByParent(id).option.liftEffect

      override def getBestAt(height: HeaderHeight): CI[Option[HeaderDBModel]] =
        HeadersQueries.getBestAt(height).option.liftEffect

      override def insert(header: HeaderDBModel): CI[Int] =
        HeadersQueries.insert(header).liftEffect

      override def updateBestChainField(id: Id, statement: Boolean): CI[Int] =
        HeadersQueries.updateBestChainField(id, statement).run.liftEffect

      override def getBestHeight: CI[Option[Int]] =
        HeadersQueries.getBestHeight.option.liftEffect

      override def getLast(quantity: Int): CI[List[Id]] =
        HeadersQueries.getLast(quantity).to[List].liftEffect
    }
}
