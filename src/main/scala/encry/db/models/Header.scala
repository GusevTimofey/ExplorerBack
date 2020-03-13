package encry.db.models

import encry._

final case class Header(
  id: Id,
  version: Version,
  parentId: Id,
  transactionsRoot: TransactionRoot,
  timestamp: Timestamp,
  height: HeaderHeight,
  nonce: Nonce,
  difficulty: Difficulty,
  stateRoot: StateRoot,
  equihashSolution: List[Int]
)
