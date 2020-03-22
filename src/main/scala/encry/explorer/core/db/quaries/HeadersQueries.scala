package encry.explorer.core.db.quaries

import doobie.implicits._
import doobie.util.query.Query0
import doobie.util.update.Update0
import encry.explorer.core.db.models.Header
import encry.explorer.core.{ HeaderHeight, Id }
//todo This import has to be declared in the scope. Doesn't compile without it. Intellij IDEA bug.
//todo import doobie.postgres.implicits._
import doobie.postgres.implicits._

object HeadersQueries extends QueriesFrame {

  override val table: String = "HEADERS"

  override val fields: List[String] =
    List(
      "id",
      "parent_id",
      "txs_root",
      "state_root",
      "version",
      "height",
      "difficulty",
      "timestamp",
      "nonce",
      "equihash_solution",
      "txs_count",
      "miner_address",
      "is_in_best_chain"
    )

  def getBy(id: Id): Query0[Header] =
    sql"""SELECT * FROM $table WHERE id = ${id.getValue}""".query[Header]

  def getBy(height: HeaderHeight): Query0[Header] =
    sql"""SELECT * FROM $table WHERE height = ${height.value}""".query[Header]

  def getByParent(id: Id): Query0[Header] =
    sql"""SELECT * FROM $table WHERE parent_id = ${id.getValue}""".query[Header]

  def getBestAt(height: HeaderHeight): Query0[Header] =
    sql"""SELECT * FROM $table WHERE height = ${height.value} AND is_in_best_chain = TRUE""".query[Header]

  def insert(header: Header): Update0 =
    sql"""INSERT INTO $table ($fieldsToQuery) VALUES ($valuesToQuery) ON CONFLICT DO NOTHING""".update

  def updateBestChainField(id: Id, statement: Boolean): Update0 =
    sql"""UPDATE $table is_in_best_chain = $statement WHERE id = ${id.getValue}""".update

}
