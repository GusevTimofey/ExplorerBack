package encry.explorer.chain.observer.http.api.models

import encry.explorer.core.Id

final case class HttpApiPayload(headerId: Id, transactions: List[HttpApiTransaction])
