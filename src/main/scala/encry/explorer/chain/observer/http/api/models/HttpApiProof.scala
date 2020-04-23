package encry.explorer.chain.observer.http.api.models

import io.circe.generic.JsonCodec

@JsonCodec final case class HttpApiProof(serializedValue: String, tagOpt: Option[String])
