package encry.explorer.chain.observer.http.api.models

import cats.kernel.Monoid
import encry.explorer.core.Id
import cats.instances.try_._
import scala.util.Try

final case class HttpApiPayload(headerId: Id, transactions: List[HttpApiTransaction])

object HttpApiPayload {
  object instances {
    implicit object HttpApiPayloadMonoid extends Monoid[HttpApiPayload] {
      override def empty: HttpApiPayload =
        HttpApiPayload(
          Id.fromString[Try]("").get,
          List.empty[HttpApiTransaction]
        )

      override def combine(x: HttpApiPayload, y: HttpApiPayload): HttpApiPayload = x
    }
  }
}
