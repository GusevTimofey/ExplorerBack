package encry.explorer.chain.observer.http.api.models.boxes

import cats.syntax.functor._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.{ Decoder, Encoder }

trait HttpApiBox

object HttpApiBox {
  implicit val encodeEvent: Encoder[HttpApiBox] = Encoder.instance {
    case ab: HttpApiAssetBox         => ab.asJson
    case db: HttpApiDataBox          => db.asJson
    case tib: HttpApiTokenIssuingBox => tib.asJson
  }

  implicit val decodeEvent: Decoder[HttpApiBox] =
    List[Decoder[HttpApiBox]](
      Decoder[HttpApiAssetBox].widen,
      Decoder[HttpApiDataBox].widen,
      Decoder[HttpApiTokenIssuingBox].widen,
    ).reduceLeft(_ or _)
}
