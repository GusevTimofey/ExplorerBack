package encry.explorer.core.db.models.boxes

import encry.explorer.core._

final case class TokenIssuingBox(
  proposition: EncryProposition,
  nonce: Nonce,
  amount: Amount,
  tokenId: TokenId
) extends Box