package encry.explorer.chain.observer.http.api.models.directives

import encry.explorer.chain.observer.TypeId
import encry.explorer.core._

final case class HttpApiTransferDirective(
  typeId: TypeId,
  address: Address,
  amount: Amount,
  tokenIdOpt: Option[TokenId]
) extends HttpApiDirective
