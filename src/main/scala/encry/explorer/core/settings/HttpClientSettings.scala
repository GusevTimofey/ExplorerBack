package encry.explorer.core.settings

final case class HttpClientSettings(
  encryNodes: List[String],
  threshold: Int,
  blockRequestInterval: Int
)
