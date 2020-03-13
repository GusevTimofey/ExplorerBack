package encry.db.models

import encry._
import encry.db.models.boxes.Box
import encry.db.models.directives.Directive

final case class Transaction(
  id: Id,
  fee: TxFee,
  timestamp: Timestamp,
  inputs: List[Input],
  outputs: List[Box],
  defaultProofOpt: Option[Proof],
  directive: List[Directive]
)
