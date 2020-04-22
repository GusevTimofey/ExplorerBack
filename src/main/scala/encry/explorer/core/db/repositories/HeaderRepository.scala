package encry.explorer.core.db.repositories

import cats.tagless._
import cats.tagless.implicits._
import cats.~>
import doobie.free.connection.ConnectionIO
import encry.explorer.core.db.models.HeaderDBModel
import encry.explorer.core.db.queries.HeadersQueries
import encry.explorer.core.{ Id, _ }

@autoFunctorK
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

  private object hr extends HeaderRepository[ConnectionIO] {
    override def getBy(id: Id): ConnectionIO[Option[HeaderDBModel]] =
      HeadersQueries.getBy(id).option

    override def getByH(height: HeaderHeight): ConnectionIO[Option[HeaderDBModel]] =
      HeadersQueries.getByH(height).option

    override def getByParent(id: Id): ConnectionIO[Option[HeaderDBModel]] =
      HeadersQueries.getByParent(id).option

    override def getBestAt(height: HeaderHeight): ConnectionIO[Option[HeaderDBModel]] =
      HeadersQueries.getBestAt(height).option

    override def insert(header: HeaderDBModel): ConnectionIO[Int] =
      HeadersQueries.insert(header)

    override def updateBestChainField(id: Id, statement: Boolean): ConnectionIO[Int] =
      HeadersQueries.updateBestChainField(id, statement).run

    override def getBestHeight: ConnectionIO[Option[Int]] =
      HeadersQueries.getBestHeight.option

    override def getLast(quantity: Int): ConnectionIO[List[Id]] =
      HeadersQueries.getLast(quantity).to[List]
  }

  def apply[CI[_]](fk: ConnectionIO ~> CI): HeaderRepository[CI] = hr.mapK(fk)
}
