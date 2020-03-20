package encry.explorer.core.db.models.directives

import encry.explorer.core._

final case class AssetIssuingDirective(contractHash: ContractHash, amount: Amount) extends Directive
