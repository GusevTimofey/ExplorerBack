package encry.explorer.core.db.quaries

import doobie.free.connection.ConnectionIO
import encry.explorer.core.Id
import encry.explorer.core.db.models.boxes.{AssetBox, Box}
import doobie.implicits._
import doobie.postgres.implicits._
import encry.explorer.core._
import encry.explorer.core.db.models.Output

object BoxQueries {

  def get(id: Id): ConnectionIO[Option[Output]] =
    sql"""select * from box where id = ${id.value.value}""".query[Output].option

}
