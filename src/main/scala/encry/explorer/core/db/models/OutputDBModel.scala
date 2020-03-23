package encry.explorer.core.db.models

import encry.explorer.core._

final case class OutputDBModel(
  id: Id,
  transactionId: Id,
  outputTypeId: Byte,
  contractHash: ContractHash,
  isActive: Boolean,
  nonce: Nonce,
  amount: Amount,
  data: String,
  tokenId: String
)
