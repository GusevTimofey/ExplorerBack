package encry.explorer.chain.observer.http.api.models

import io.circe.generic.JsonCodec

@JsonCodec final case class HttpApiPeersInfo(address: String, name: String, connectionType: String)
