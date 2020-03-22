package encry.explorer.core.db.quaries

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.util.query.Query0
import encry.explorer.core.Id
import encry.explorer.core.db.models.Output
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
    sql"""select * from box where id = ${id.value.value}""".query[Output]

}
