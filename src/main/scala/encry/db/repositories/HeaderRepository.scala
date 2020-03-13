package encry.db.repositories

import cats.effect.Bracket
import doobie.hikari.HikariTransactor
import encry.Id
import encry.db.models.Header
import encry.db.quaries.HeaderQueries
import doobie.implicits._

trait HeaderRepository[F[_]] {

  def get(id: Id): F[Option[Header]]
}

object HeaderRepository {

  def apply[F[_]: Bracket[*[_], Throwable]](tx: HikariTransactor[F]): HeaderRepository[F] =
    (id: Id) => HeaderQueries.get(id).transact[F](tx)
}
