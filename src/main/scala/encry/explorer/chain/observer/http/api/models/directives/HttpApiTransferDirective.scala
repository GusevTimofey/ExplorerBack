package encry.explorer.chain.observer.http.api.models.directives

import encry.explorer.chain.observer.TypeId
import encry.explorer.core._

final case class HttpApiTransferDirective(
  typeId: TypeId,
  address: Address,
  amount: Amount,
  tokenId: String //todo bug in encry protocol. see https://github.com/EncryFoundation/EncryCommon/pull/22
) extends HttpApiDirective
