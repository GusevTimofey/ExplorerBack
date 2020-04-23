package encry.explorer.chain.observer.http.api.models

import encry.explorer.core._
import io.circe.generic.JsonCodec

@JsonCodec final case class HttpApiHeader(
  id: Id,
  version: Version,
  parentId: Id,
  payloadId: Id,
  txRoot: TransactionRoot,
  timestamp: Timestamp,
  height: HeaderHeight,
  nonce: Nonce,
  difficulty: Difficulty,
  equihashSolution: List[Int],
  stateRoot: StateRoot
)
