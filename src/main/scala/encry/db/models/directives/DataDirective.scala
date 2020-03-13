package encry.db.models.directives

import encry.{ ContractHash, Data }

final case class DataDirective(contractHash: ContractHash, data: Data) extends Directive
