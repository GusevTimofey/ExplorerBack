package encry.explorer.chain.observer.http.api.models.boxes

import cats.syntax.either._
import encry.explorer.chain.observer.TypeId
import encry.explorer.core._
import io.circe.generic.JsonCodec
import io.circe.syntax._
import io.circe.{ Decoder, DecodingFailure, Encoder }

sealed trait HttpApiBox

object HttpApiBox {

  @JsonCodec final case class HttpApiAssetBox(
    `type`: TypeId,
    id: Id,
    proposition: EncryProposition,
    nonce: Nonce,
    value: Amount,
    tokenId: Option[TokenId]
  ) extends HttpApiBox

  object HttpApiAssetBox { val HttpApiAssetBoxTypeId: TypeId = TypeId(1: Byte) }

  @JsonCodec final case class HttpApiDataBox(
    `type`: TypeId,
    id: Id,
    proposition: EncryProposition,
    nonce: Nonce,
    data: Data
  ) extends HttpApiBox

  object HttpApiDataBox { val HttpApiTokenIssuingBoxTypeId: TypeId = TypeId(3: Byte) }

  @JsonCodec final case class HttpApiTokenIssuingBox(
    `type`: TypeId,
    id: Id,
    tokenId: TokenId,
    proposition: EncryProposition,
    nonce: Nonce,
    amount: Amount
  ) extends HttpApiBox

  object HttpApiTokenIssuingBox { val HttpApiDataBoxTypeId: TypeId = TypeId(4: Byte) }

  // есть перечисление всех возможных вариантов. Не может быть non exhaustive.
  implicit val encodeEvent: Encoder[HttpApiBox] = Encoder.instance {
    case ab: HttpApiAssetBox         => ab.asJson
    case db: HttpApiDataBox          => db.asJson
    case tib: HttpApiTokenIssuingBox => tib.asJson
  }

  import HttpApiAssetBox._
  import HttpApiDataBox._
  import HttpApiTokenIssuingBox._

  implicit val decodeEvent: Decoder[HttpApiBox] =
    Decoder.instance { c =>
      c.get[TypeId]("type") match {
        case Right(id) =>
          id match {
            case HttpApiAssetBoxTypeId        => c.value.as[HttpApiAssetBox]
            case HttpApiDataBoxTypeId         => c.value.as[HttpApiDataBox]
            case HttpApiTokenIssuingBoxTypeId => c.value.as[HttpApiTokenIssuingBox]
            case _                            => DecodingFailure("Unknown box type id", c.history).asLeft[HttpApiBox]
          }
        case Left(_) => DecodingFailure("Empty box type id value", c.history).asLeft[HttpApiBox]
      }
    }
}
