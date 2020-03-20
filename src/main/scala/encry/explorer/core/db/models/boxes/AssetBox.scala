package encry.explorer.core.db.models.boxes

import encry.explorer.core._

final case class AssetBox(
  proposition: EncryProposition,
  nonce: Nonce,
  amount: Amount,
  tokenIdOpt: Option[TokenId]
) extends Box