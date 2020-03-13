package encry.db.models

import encry.Id

final case class Payload(headerId: Id, txs: List[Transaction])
