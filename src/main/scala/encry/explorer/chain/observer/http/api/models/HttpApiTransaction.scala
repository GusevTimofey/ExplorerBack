package encry.explorer.chain.observer.http.api.models

import encry.explorer.chain.observer.http.api.models.boxes.HttpApiBox
import encry.explorer.chain.observer.http.api.models.directives.HttpApiDirective
import encry.explorer.core._

final case class HttpApiTransaction(
  id: Id,
  fee: Amount,
  timestamp: Timestamp,
  inputs: List[HttpApiInput],
  directives: List[HttpApiDirective],
  outputs: List[HttpApiBox],
  defaultProofOpt: Option[HttpApiProof]
)
