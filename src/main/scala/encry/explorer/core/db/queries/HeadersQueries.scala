package encry.explorer.core.db.queries

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.util.query.Query0
import doobie.util.update.{ Update, Update0 }
import encry.explorer.core.db.models.HeaderDBModel
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

  def getBy(id: Id): Query0[HeaderDBModel] =
    sql"""SELECT * FROM $table WHERE id = ${id.getValue}""".query[HeaderDBModel]

  def getByH(height: HeaderHeight): Query0[HeaderDBModel] =
    sql"""SELECT * FROM $table WHERE height = ${height.value}""".query[HeaderDBModel]

  def getByParent(id: Id): Query0[HeaderDBModel] =
    sql"""SELECT * FROM $table WHERE parent_id = ${id.getValue}""".query[HeaderDBModel]

  def getBestAt(height: HeaderHeight): Query0[HeaderDBModel] =
    sql"""SELECT * FROM $table WHERE height = ${height.value} AND is_in_best_chain = TRUE""".query[HeaderDBModel]

  def insert(header: HeaderDBModel): ConnectionIO[Int] = {
    val sql = s"INSERT INTO $table ($fieldsToQuery) VALUES ($valuesToQuery) ON CONFLICT DO NOTHING"
    Update[HeaderDBModel](sql).run(header)
  }

  def updateBestChainField(id: Id, statement: Boolean): Update0 =
    sql"""UPDATE $table is_in_best_chain = $statement WHERE id = ${id.getValue}""".update

}
