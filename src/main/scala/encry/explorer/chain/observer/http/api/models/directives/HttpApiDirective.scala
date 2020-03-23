package encry.explorer.chain.observer.http.api.models.directives

import cats.syntax.functor._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.{ Decoder, Encoder }

trait HttpApiDirective

object HttpApiDirective {

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
