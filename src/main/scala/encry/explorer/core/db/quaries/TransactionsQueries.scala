package encry.explorer.core.db.quaries

import doobie.implicits._
import doobie.util.query.Query0
import doobie.util.update.Update0
import encry.explorer.core.Id
import encry.explorer.core.db.models.TransactionDBModel
//todo This import has to be declared in the scope. Doesn't compile without it. Intellij IDEA bug.
//todo import doobie.postgres.implicits._
import doobie.postgres.implicits._

object TransactionsQueries extends QueriesFrame {

  override val fields: List[String] =
    List(
      "id",
      "header_id",
      "fee",
      "timestamp",
      "proof",
      "is_coinbase_tx"
    )

  override val table: String = "TRANSACTIONS"

  def getBy(id: Id): Query0[TransactionDBModel] =
    sql"""SELECT * FROM $table WHERE id = ${id.getValue}""".query[TransactionDBModel]

  def getByHeader(id: Id): Query0[TransactionDBModel] =
    sql"""SELECT * FROM $table WHERE header_id = ${id.getValue}""".query[TransactionDBModel]

  def insert(transaction: TransactionDBModel): Update0 =
    sql"""INSERT INTO $table ($fieldsToQuery) VALUES ($valuesToQuery) ON CONFLICT DO NOTHING""".update

}
