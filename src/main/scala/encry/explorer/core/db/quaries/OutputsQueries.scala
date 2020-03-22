package encry.explorer.core.db.quaries

import cats.instances.list._
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.util.query.Query0
import doobie.util.update.Update
import encry.explorer.core.db.models.Output
import encry.explorer.core.{ ContractHash, Id }
//todo This import has to be declared in the scope. Doesn't compile without it. Intellij IDEA bug.
//todo import doobie.postgres.implicits._
import doobie.postgres.implicits._

object OutputsQueries extends QueriesFrame {

  override val fields: List[String] =
    List(
      "id",
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

  def getBy(id: Id): Query0[Output] =
    sql"""SELECT * FROM $table WHERE id = ${id.getValue}""".query[Output]

  def getByC(contractHash: ContractHash): Query0[Output] =
    sql"""SELECT * FROM $table WHERE contract_hash = ${contractHash.getValue}""".query[Output]

  def getByTransaction(id: Id): Query0[Output] =
    sql"""SELECT * FROM $table WHERE tx_id = ${id.getValue}""".query[Output]

  def insertMany(outputs: List[Output]): ConnectionIO[Int] = {
    val sql: String = s"INSERT INTO $table ($fieldsToQuery) VALUES ($valuesToQuery) ON CONFLICT DO NOTHING"
    Update[Output](sql).updateMany(outputs)
  }

}
