package encry.explorer.core.db.models.directives

import encry.explorer.core._

final case class ScriptedAssetDirective(
  contractHash: ContractHash,
  amount: Amount,
  tokenIdOpt: Option[TokenId]
) extends Directive
