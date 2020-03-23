package encry.explorer.chain.observer.http.api.models.directives

import encry.explorer.chain.observer.TypeId
import encry.explorer.core._

final case class HttpApiDataDirective(
  typeId: TypeId,
  contractHash: ContractHash,
  data: Data
) extends HttpApiDirective
