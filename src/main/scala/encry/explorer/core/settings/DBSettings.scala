package encry.explorer.core.settings

final case class DBSettings(
  connectionsPoolSize: Int,
  poolName: String,
  jdbcDriver: String,
  dbUrl: String,
  login: String,
  password: String
)
