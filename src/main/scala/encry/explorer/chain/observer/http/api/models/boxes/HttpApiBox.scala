package encry.explorer.chain.observer.http.api.models.boxes

import cats.syntax.functor._
import encry.explorer.chain.observer.TypeId
import encry.explorer.core.{Amount, Data, Id, Nonce, TokenId}
import io.circe.{Decoder, Encoder}
import io.circe.syntax._
import io.circe.generic.JsonCodec

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

  @JsonCodec final case class HttpApiDataBox(
    `type`: TypeId,
    id: Id,
    proposition: EncryProposition,
    nonce: Nonce,
    data: Data
  ) extends HttpApiBox

  @JsonCodec final case class HttpApiTokenIssuingBox(
    `type`: TypeId,
    id: Id,
    tokenId: TokenId,
    proposition: EncryProposition,
    nonce: Nonce,
    amount: Amount
  ) extends HttpApiBox

  implicit val encodeEvent: Encoder[HttpApiBox] = Encoder.instance {
    case ab: HttpApiAssetBox         => ab.asJson
    case db: HttpApiDataBox          => db.asJson
    case tib: HttpApiTokenIssuingBox => tib.asJson
  }

  implicit val decodeEvent: Decoder[HttpApiBox] =
    List[Decoder[HttpApiBox]](
      Decoder[HttpApiAssetBox].widen,
      Decoder[HttpApiDataBox].widen,
      Decoder[HttpApiTokenIssuingBox].widen
    ).reduceLeft(_ or _)
}
