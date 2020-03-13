package encry.db.models.directives

import encry._

final case class ScriptedAssetDirective(
  contractHash: ContractHash,
  amount: Amount,
  tokenIdOpt: Option[TokenId]
) extends Directive
