package encry.explorer.core.db.quaries

import doobie.free.connection.ConnectionIO
import encry.explorer.core.Id
import encry.explorer.core.db.models.boxes.Box
import doobie.implicits._
import encry.explorer.core.db.models.Payload

object PayloadQueries {
  def get(id: Id): ConnectionIO[Option[Payload]] =
    sql"""select * from payload where id = ${id.value}""".query[Payload].option
}
