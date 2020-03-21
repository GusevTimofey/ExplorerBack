package encry.explorer.core.db.models

import encry.explorer.core._

final case class Input(
  transactionId: Id,
  boxId: Id,
  proofs: String,
  contract: String,
)
