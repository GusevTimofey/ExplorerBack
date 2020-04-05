package encry.explorer.core.settings

final case class ChainObserverSettings(
  encryNodes: List[String],
  observerClientThreadsQuantity: Int,
  maxConnections: Int,
  blockRequestInterval: Int
)
