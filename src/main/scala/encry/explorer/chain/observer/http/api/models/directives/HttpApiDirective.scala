package encry.explorer.chain.observer.http.api.models.directives

import cats.syntax.either._
import encry.explorer.chain.observer.TypeId
import encry.explorer.core._
import io.circe.generic.JsonCodec
import io.circe.syntax._
import io.circe.{ Decoder, DecodingFailure, Encoder }

sealed trait HttpApiDirective

object HttpApiDirective {

  @JsonCodec final case class HttpApiAssetIssuingDirective(
    typeId: TypeId,
    contractHash: ContractHash,
    amount: Amount
  ) extends HttpApiDirective

  object HttpApiAssetIssuingDirective { val HttpApiTransferDirectiveTypeId: TypeId = TypeId(1: Byte) }

  @JsonCodec final case class HttpApiDataDirective(
    typeId: TypeId,
    contractHash: ContractHash,
    data: Data
  ) extends HttpApiDirective

  object HttpApiDataDirective { val HttpApiAssetIssuingDirectiveTypeId: TypeId = TypeId(2: Byte) }

  @JsonCodec final case class HttpApiScriptedAssetDirective(
    typeId: TypeId,
    contractHash: ContractHash,
    amount: Amount,
    tokenId: Option[TokenId]
  ) extends HttpApiDirective

  object HttpApiScriptedAssetDirective { val HttpApiScriptedAssetDirectiveTypeId: TypeId = TypeId(3: Byte) }

  @JsonCodec final case class HttpApiTransferDirective(
    typeId: TypeId,
    address: Address,
    amount: Amount,
    tokenId: String //todo bug in encry protocol. see https://github.com/EncryFoundation/EncryCommon/pull/22
  ) extends HttpApiDirective

  object HttpApiTransferDirective { val HttpApiDataDirectiveTypeId: TypeId = TypeId(4: Byte) }

  // есть перечисление всех возможных вариантов. Не может быть non exhaustive.
  implicit val encodeEvent: Encoder[HttpApiDirective] = Encoder.instance {
    case ai: HttpApiAssetIssuingDirective  => ai.asJson
    case d: HttpApiDataDirective           => d.asJson
    case sa: HttpApiScriptedAssetDirective => sa.asJson
    case t: HttpApiTransferDirective       => t.asJson
  }

  import HttpApiAssetIssuingDirective._
  import HttpApiDataDirective._
  import HttpApiScriptedAssetDirective._
  import HttpApiTransferDirective._

  implicit val decodeEvent: Decoder[HttpApiDirective] =
    Decoder.instance { c =>
      c.get[TypeId]("typeId") match {
        case Right(typeId) =>
          typeId match {
            case HttpApiTransferDirectiveTypeId      => c.value.as[HttpApiTransferDirective]
            case HttpApiAssetIssuingDirectiveTypeId  => c.value.as[HttpApiAssetIssuingDirective]
            case HttpApiDataDirectiveTypeId          => c.value.as[HttpApiDataDirective]
            case HttpApiScriptedAssetDirectiveTypeId => c.value.as[HttpApiScriptedAssetDirective]
            case _                                   => DecodingFailure("Unknown directive type id", c.history).asLeft[HttpApiDirective]
          }
        case Left(_) => DecodingFailure("Empty directive type id value", c.history).asLeft[HttpApiDirective]
      }
    }
}
