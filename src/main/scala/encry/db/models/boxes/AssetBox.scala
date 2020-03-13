package encry.db.models.boxes

import encry.{ Amount, Nonce, TokenId }

final case class AssetBox(
  proposition: EncryProposition,
  nonce: Nonce,
  amount: Amount,
  tokenIdOpt: Option[TokenId]
) extends Box