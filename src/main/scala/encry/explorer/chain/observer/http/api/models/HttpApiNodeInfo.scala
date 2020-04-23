package encry.explorer.chain.observer.http.api.models

import io.circe.generic.JsonCodec

@JsonCodec final case class HttpApiNodeInfo(
  name: String,
  stateType: String,
  difficulty: Int,
  bestFullHeaderId: String,
  bestHeaderId: String,
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
