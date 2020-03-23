package encry.explorer.chain.observer.http.api.models.directives

import encry.explorer.chain.observer.TypeId
import encry.explorer.core._

final case class HttpApiScriptedAssetDirective(
  typeId: TypeId,
  contractHash: ContractHash,
  amount: Amount,
  tokenIdOpt: Option[TokenId]
) extends HttpApiDirective
