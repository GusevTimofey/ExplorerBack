package encry.explorer.chain.observer.http.api.models.boxes

import encry.explorer.chain.observer.TypeId
import encry.explorer.core._

final case class HttpApiDataBox(
  `type`: TypeId,
  id: Id,
  proposition: EncryProposition,
  nonce: Nonce,
  data: Data
) extends HttpApiBox