package encry.explorer.core.db.queries

import doobie.util.log.LogHandler

trait QueriesFrame {

  implicit val han: LogHandler = LogHandler.jdkLogHandler

  val table: String

  val fields: List[String]

  val fieldsToQuery: String = fields.mkString(", ")

  val valuesToQuery: String = fields.map(_ => "?").mkString(", ")

}
