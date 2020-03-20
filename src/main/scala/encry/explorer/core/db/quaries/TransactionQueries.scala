package encry.explorer.core.db.quaries

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import encry.explorer.core.Id
import encry.explorer.core.db.models.Transaction

object TransactionQueries {

  def get(id: Id): ConnectionIO[Option[Transaction]] =
    sql"""select * from transactions where id = ${id.value}""".query[Transaction].option

}
