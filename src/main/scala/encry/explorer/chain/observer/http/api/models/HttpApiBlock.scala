package encry.explorer.chain.observer.http.api.models

import cats.Monoid

final case class HttpApiBlock(header: HttpApiHeader, payload: HttpApiPayload)

object HttpApiBlock {
  object instances {
    implicit object HttpApiBlockMonoid extends Monoid[HttpApiBlock] {
      override def empty: HttpApiBlock =
        HttpApiBlock(
          HttpApiHeader.instances.HttpApiHeaderMonoid.empty,
          HttpApiPayload.instances.HttpApiPayloadMonoid.empty
        )

      override def combine(x: HttpApiBlock, y: HttpApiBlock): HttpApiBlock = x
    }
  }
}
