package encry.explorer.env

import cats.effect.{ Async, ContextShift, Resource }
import doobie.hikari.HikariTransactor
import encry.explorer.core.db.DB
import encry.explorer.core.db.algebra.LiftConnectionIO
import encry.explorer.core.settings.DBSettings

final case class DBContext[CI[_], F[_]](
  repositoriesContext: RepositoriesContext[CI],
  transactor: HikariTransactor[F]
)

object DBContext {
  def create[F[_]: ContextShift: Async, CI[_]: LiftConnectionIO](
    dBSettings: DBSettings
  ): Resource[F, DBContext[CI, F]] =
    DB[F](dBSettings).map { ht: HikariTransactor[F] => DBContext(RepositoriesContext.create[CI], ht) }
}
