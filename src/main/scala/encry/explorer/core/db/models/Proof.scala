package encry.explorer.core.db.models

import encry.explorer.core._

final case class Proof(serializedValue: SerializedProofValue, tag: Option[String])
