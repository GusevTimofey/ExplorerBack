package encry.explorer.core.db.queries

import cats.instances.list._
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.util.query.Query0
import doobie.util.update.Update
import encry.explorer.core.db.models.OutputDBModel
import encry.explorer.core.{ ContractHash, Id }
//todo This import has to be declared in the scope. Doesn't compile without it. Intellij IDEA bug.
//todo import doobie.postgres.implicits._
import doobie.postgres.implicits._

object OutputsQueries extends QueriesFrame {

  override val fields: List[String] =
    List(
      "id",
      "header_id",
      "tx_id",
      "output_type_id",
      "contract_hash",
      "is_active",
      "nonce",
      "amount",
      "data",
      "token_id"
    )

  override val table: String = "OUTPUTS"

  def getBy(id: Id): Query0[OutputDBModel] =
    sql"""SELECT * FROM $table WHERE id = ${id.getValue}""".query[OutputDBModel]

  def getByC(contractHash: ContractHash): Query0[OutputDBModel] =
    sql"""SELECT * FROM $table WHERE contract_hash = ${contractHash.getValue}""".query[OutputDBModel]

  def getByTransaction(id: Id): Query0[OutputDBModel] =
    sql"""SELECT * FROM $table WHERE tx_id = ${id.getValue}""".query[OutputDBModel]

  def insertMany(outputs: List[OutputDBModel]): ConnectionIO[Int] = {
    val sql: String = s"INSERT INTO $table ($fieldsToQuery) VALUES ($valuesToQuery) ON CONFLICT DO NOTHING"
    Update[OutputDBModel](sql).updateMany(outputs)
  }

}
