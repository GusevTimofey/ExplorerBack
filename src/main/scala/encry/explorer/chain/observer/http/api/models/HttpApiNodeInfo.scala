package encry.explorer.chain.observer.http.api.models

import cats.Monoid

final case class HttpApiNodeInfo(
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

object HttpApiNodeInfo {
  object instances {
    implicit object HttpApiNodeInfoMonoid extends Monoid[HttpApiNodeInfo] {
      override def empty: HttpApiNodeInfo =
        HttpApiNodeInfo(
          "",
          "",
          -1,
          "",
          "",
          -1,
          -1,
          "",
          -1,
          -1,
          "",
          -1L,
          "",
          isConnectedWithKnownPeers = false,
          isMining = false,
          Seq.empty,
          ""
        )

      override def combine(x: HttpApiNodeInfo, y: HttpApiNodeInfo): HttpApiNodeInfo = x
    }
  }
}
