package encry.explorer.core.db.quaries

import doobie.util.query.Query0
import encry.explorer.core._
import doobie.implicits._
import cats.instances.list._
import doobie.free.connection.ConnectionIO
import doobie.util.update.{ Update, Update0 }
import encry.explorer.core.db.models.Input
//todo This import has to be declared in the scope. Doesn't compile without it. Intellij IDEA bug.
//todo import doobie.postgres.implicits._
import doobie.postgres.implicits._

object InputsQueries extends QueriesFrame {

  override val fields: List[String] =
    List(
      "tx_id",
      "box_id",
      "proofs",
      "contract"
    )

  override val table: String = "INPUTS"

  def getBy(id: Id): Query0[Input] =
    sql"""SELECT * FROM $table WHERE box_id = ${id.getValue}""".query[Input]

  def getByTransaction(id: Id): Query0[Input] =
    sql"""SELECT * FROM $table WHERE tx_id = ${id.getValue}""".query[Input]

  def insertMany(inputs: List[Input]): ConnectionIO[Int] = {
    val sql: String = s"INSERT INTO $table ($fieldsToQuery) VALUES ($valuesToQuery) ON CONFLICT DO NOTHING"
    Update[Input](sql).updateMany(inputs)
  }

  def updateIsActiveField(id: Id, isActive: Boolean): Update0 =
    sql"""UPDATE $table is_active = $isActive WHERE box_id = ${id.getValue}""".update
}
