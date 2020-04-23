package encry.explorer.chain.observer.http.api.models

import encry.explorer.core.Id
import io.circe.generic.JsonCodec

@JsonCodec final case class HttpApiPayload(headerId: Id, transactions: List[HttpApiTransaction])
