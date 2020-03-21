package encry.explorer.core.db.quaries

import doobie.implicits._
import doobie.util.query.Query0
import doobie.util.update.Update0
import encry.explorer.core.{ HeaderHeight, Id }
import encry.explorer.core.db.models.Header
//todo This import has to be declared in the scope. Doesn't compile without it. Intellij IDEA bug.
//todo import doobie.postgres.implicits._
import doobie.postgres.implicits._

object HeaderQueries extends DBQueries {

  def getBy(id: Id): Query0[Header] =
    sql"""SELECT * FROM HEADERS where id = ${id.getValue}""".query[Header]

  def getBy(height: HeaderHeight): Query0[Header] =
    sql"""SELECT * FROM HEADERS where height = ${height.value}""".query[Header]

  def getBestAt(height: HeaderHeight): Query0[Header] =
    sql"""SELECT * FROM HEADERS where height = ${height.value} AND is_in_best_chain = TRUE""".query[Header]

  def getByParent(id: Id): Query0[Header] =
    sql"""SELECT * FROM HEADERS WHERE parent_id = ${id.getValue}""".query[Header]

  //todo
  def insertHeader(header: Header): Update0 =
    sql"""INSERT INTO HEADERS (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""".stripMargin.update

}
