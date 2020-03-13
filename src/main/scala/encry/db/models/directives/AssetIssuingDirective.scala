package encry.db.models.directives

import encry._

final case class AssetIssuingDirective(contractHash: ContractHash, amount: Amount) extends Directive
