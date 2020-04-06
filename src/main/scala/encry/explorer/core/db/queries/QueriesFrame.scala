package encry.explorer.core.db.queries

import doobie.util.log.LogHandler

trait QueriesFrame {

  //todo implement custom logger for clean info output
  implicit val han: LogHandler = LogHandler.jdkLogHandler

  val table: String

  val fields: List[String]

  def fieldsToQuery: String = fields.mkString(", ")

  def valuesToQuery: String = fields.map(_ => "?").mkString(", ")

}
