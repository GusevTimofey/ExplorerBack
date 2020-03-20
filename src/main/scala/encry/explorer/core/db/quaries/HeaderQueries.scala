package encry.explorer.core.db.quaries

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import encry.explorer.core.Id
import encry.explorer.core.db.models.Header
//todo import below has to be declared in the scope. Doesn't compile without it.
//todo import doobie.postgres.implicits._
import doobie.postgres.implicits._

object HeaderQueries extends DBQueries {

  def get(id: Id): ConnectionIO[Option[Header]] =
    sql"""select * header where id = ${id.value}""".query[Header].option
}
