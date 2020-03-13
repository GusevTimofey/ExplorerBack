package encry.db.models.boxes

import encry.{ Data, Nonce }

final case class DataBox(
  proposition: EncryProposition,
  nonce: Nonce,
  data: Data
) extends Box