package encry.explorer.core.db.quaries

import doobie.free.connection.ConnectionIO
import encry.explorer.core.Id
import encry.explorer.core.db.models.boxes.{AssetBox, Box}
import doobie.implicits._

object BoxQueries {

  def get(id: Id): ConnectionIO[Option[AssetBox]] =
    sql"""select * from box where id = ${id.value}""".query[AssetBox].option

}
