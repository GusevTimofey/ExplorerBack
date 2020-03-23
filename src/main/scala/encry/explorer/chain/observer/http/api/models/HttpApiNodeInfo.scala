package encry.explorer.chain.observer.http.api.models

final case class HttpApiNodeInfo(
  name: String,
  stateType: Byte,
  difficulty: Int,
  bestFullHeaderId: Int,
  bestHeaderId: Int,
  peersCount: Int,
  unconfirmedCount: Int,
  previousFullHeaderId: String,
  fullHeight: Int,
  headersHeight: Int,
  stateVersion: String,
  uptime: Long,
  storage: String,
  isConnectedWithKnownPeers: Boolean,
  isMining: Boolean,
  knownPeers: Seq[String],
  stateRoot: String
)
