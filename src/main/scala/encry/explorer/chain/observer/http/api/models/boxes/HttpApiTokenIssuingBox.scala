package encry.explorer.chain.observer.http.api.models.boxes

import encry.explorer.chain.observer.TypeId
import encry.explorer.core._

final case class HttpApiTokenIssuingBox(
  `type`: TypeId,
  id: Id,
  tokenId: TokenId,
  proposition: EncryProposition,
  nonce: Nonce,
  amount: Amount
) extends HttpApiBox
