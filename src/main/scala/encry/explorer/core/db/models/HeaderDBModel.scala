package encry.explorer.core.db.models

import encry.explorer.core._

final case class HeaderDBModel(
  id: Id,
  parentId: Id,
  transactionsRoot: TransactionRoot,
  stateRoot: StateRoot,
  version: Version,
  height: HeaderHeight,
  difficulty: Difficulty,
  timestamp: Timestamp,
  nonce: Nonce,
  equihashSolution: List[Int],
  transactionQuantity: TransactionsQuantity,
  minerAddress: ContractHash,
  isInBestChain: Boolean
)
