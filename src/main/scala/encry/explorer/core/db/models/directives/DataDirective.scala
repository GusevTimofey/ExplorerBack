package encry.explorer.core.db.models.directives

import encry.explorer.core._

final case class DataDirective(contractHash: ContractHash, data: Data) extends Directive
