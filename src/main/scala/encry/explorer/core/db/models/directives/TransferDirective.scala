package encry.explorer.core.db.models.directives

import encry.explorer.core._

final case class TransferDirective(
  address: Address,
  amount: Amount,
  tokenIdOpt: Option[TokenId] = None
) extends Directive
