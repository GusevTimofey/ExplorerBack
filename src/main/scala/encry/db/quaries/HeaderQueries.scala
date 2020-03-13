package encry.db.quaries

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import encry.Id
import encry.db.models.Header

object HeaderQueries extends DBQueries {

  def get(id: Id): ConnectionIO[Option[Header]] =
    sql"""select * header where id = $id""".query[Header].option
}
