package encry.explorer.chain.observer.http.api.models.directives

import cats.syntax.functor._
import encry.explorer.chain.observer.TypeId
import encry.explorer.core.{ Address, Amount, ContractHash, Data, TokenId }
import io.circe.generic.JsonCodec, io.circe.syntax._
import io.circe.{ Decoder, Encoder }

sealed trait HttpApiDirective

object HttpApiDirective {

  @JsonCodec final case class HttpApiAssetIssuingDirective(
    typeId: TypeId,
    contractHash: ContractHash,
    amount: Amount
  ) extends HttpApiDirective

  @JsonCodec final case class HttpApiDataDirective(
    typeId: TypeId,
    contractHash: ContractHash,
    data: Data
  ) extends HttpApiDirective

  @JsonCodec final case class HttpApiScriptedAssetDirective(
    typeId: TypeId,
    contractHash: ContractHash,
    amount: Amount,
    tokenId: Option[TokenId]
  ) extends HttpApiDirective

  @JsonCodec final case class HttpApiTransferDirective(
    typeId: TypeId,
    address: Address,
    amount: Amount,
    tokenId: String //todo bug in encry protocol. see https://github.com/EncryFoundation/EncryCommon/pull/22
  ) extends HttpApiDirective

  implicit val encodeEvent: Encoder[HttpApiDirective] = Encoder.instance {
    case ai: HttpApiAssetIssuingDirective  => ai.asJson
    case d: HttpApiDataDirective           => d.asJson
    case sa: HttpApiScriptedAssetDirective => sa.asJson
    case t: HttpApiTransferDirective       => t.asJson
  }

  implicit val decodeEvent: Decoder[HttpApiDirective] =
    List[Decoder[HttpApiDirective]](
      Decoder[HttpApiAssetIssuingDirective].widen,
      Decoder[HttpApiDataDirective].widen,
      Decoder[HttpApiScriptedAssetDirective].widen,
      Decoder[HttpApiTransferDirective].widen
    ).reduceLeft(_ or _)
}
