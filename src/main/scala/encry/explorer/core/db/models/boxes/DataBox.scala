package encry.explorer.core.db.models.boxes

import encry.explorer.core._

final case class DataBox(
  proposition: EncryProposition,
  nonce: Nonce,
  data: Data
) extends Box