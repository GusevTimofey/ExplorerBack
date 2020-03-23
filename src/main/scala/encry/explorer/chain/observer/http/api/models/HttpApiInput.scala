package encry.explorer.chain.observer.http.api.models

import encry.explorer.chain.observer._
import encry.explorer.core._

final case class HttpApiInput(boxId: Id, contract: Contract, proofs: List[HttpApiProof])