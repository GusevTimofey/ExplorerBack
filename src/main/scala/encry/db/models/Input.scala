package encry.db.models

import encry._

final case class Input(boxId: Id, contract: InputContract, proofs: List[Proof])
