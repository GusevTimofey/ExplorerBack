package encry.explorer.core.db.models

import encry.explorer.core._

final case class Input(boxId: Id, contract: InputContract, proofs: List[Proof])
