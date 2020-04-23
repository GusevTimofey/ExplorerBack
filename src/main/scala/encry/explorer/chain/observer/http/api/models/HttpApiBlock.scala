package encry.explorer.chain.observer.http.api.models

import io.circe.generic.JsonCodec

@JsonCodec final case class HttpApiBlock(header: HttpApiHeader, payload: HttpApiPayload)