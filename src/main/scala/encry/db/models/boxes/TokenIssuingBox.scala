package encry.db.models.boxes

import encry.{ Amount, Nonce, TokenId }

final case class TokenIssuingBox(
  proposition: EncryProposition,
  nonce: Nonce,
  amount: Amount,
  tokenId: TokenId
) extends Box