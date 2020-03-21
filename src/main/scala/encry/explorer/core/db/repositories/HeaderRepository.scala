package encry.explorer.core.db.repositories

import cats.effect.Bracket
import doobie.hikari.HikariTransactor
import doobie.implicits._
import encry.explorer.core._
import encry.explorer.core.db.models.Header
import encry.explorer.core.db.quaries.HeaderQueries

trait HeaderRepository[F[_]] {

  def getBy(id: Id): F[Option[Header]]

  def getBy(height: HeaderHeight): F[Option[Header]]

  def getBestAt(height: HeaderHeight): F[Option[Header]]

  def getByParent(id: Id): F[Option[Header]]
}

object HeaderRepository {

  def apply[F[_]: Bracket[*[_], Throwable]](tx: HikariTransactor[F]): HeaderRepository[F] =
    new HeaderRepository[F] {
      override def getBy(id: Id): F[Option[Header]] =
        HeaderQueries.getBy(id).option.transact[F](tx)

      override def getBy(height: HeaderHeight): F[Option[Header]] =
        HeaderQueries.getBy(height).option.transact[F](tx)

      override def getBestAt(height: HeaderHeight): F[Option[Header]] =
        HeaderQueries.getBestAt(height).option.transact[F](tx)

      override def getByParent(id: Id): F[Option[Header]] =
        HeaderQueries.getByParent(id).option.transact[F](tx)
    }
}
