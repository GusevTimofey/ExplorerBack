package encry.explorer.core.db.models

import encry.explorer.core._

final case class Transaction(
  id: Id,
  headerId: Id,
  fee: TxFee,
  timestamp: Timestamp,
  proof: String,
  isCoinbaseTransaction: Boolean
)
