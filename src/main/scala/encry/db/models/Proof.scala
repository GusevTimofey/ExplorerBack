package encry.db.models

import encry.SerializedProofValue

final case class Proof(serializedValue: SerializedProofValue, tag: Option[String])
