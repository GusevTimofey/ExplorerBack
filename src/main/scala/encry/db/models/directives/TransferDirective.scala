package encry.db.models.directives

import encry.{ Address, Amount, TokenId }

final case class TransferDirective(
  address: Address,
  amount: Amount,
  tokenIdOpt: Option[TokenId] = None
) extends Directive
