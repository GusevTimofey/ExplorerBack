package encry.explorer.chain.observer.http.api.models

import encry.explorer.core._
import org.encryfoundation.common.utils.TaggedTypes.Difficulty

final case class HttpApiHeader(
  id: Id,
  version: Version,
  parentId: Id,
  payloadId: Id,
  transactionsRoot: TransactionRoot,
  timestamp: Timestamp,
  height: HeaderHeight,
  nonce: Nonce,
  difficulty: Difficulty,
  equihashSolution: List[Int],
  stateRoot: StateRoot
)
