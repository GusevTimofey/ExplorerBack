package encry.explorer.chain.observer.http.api.models.boxes

import encry.explorer.core._
import io.circe.generic.JsonCodec

@JsonCodec final case class EncryProposition(contractHash: ContractHash)
