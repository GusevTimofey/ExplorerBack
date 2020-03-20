package encry.explorer.core.db.models

import encry.explorer.core._
import encry.explorer.core.db.models.boxes.Box
import encry.explorer.core.db.models.directives.Directive

final case class Transaction(
  id: Id,
  fee: TxFee,
  timestamp: Timestamp,
  inputs: List[Input],
  outputs: List[Box],
  defaultProofOpt: Option[Proof],
  directive: List[Directive]
)
