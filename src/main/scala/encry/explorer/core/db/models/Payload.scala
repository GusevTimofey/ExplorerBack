package encry.explorer.core.db.models

import encry.explorer.core._

final case class Payload(headerId: Id, txs: List[Transaction])
