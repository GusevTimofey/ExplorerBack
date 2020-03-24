package encry.explorer.chain.observer.http.api.models.boxes

import encry.explorer.chain.observer.TypeId
import encry.explorer.core._

final case class HttpApiAssetBox(
  `type`: TypeId,
  id: Id,
  proposition: EncryProposition,
  nonce: Nonce,
  value: Amount,
  tokenId: Option[TokenId]
) extends HttpApiBox