package encry.explorer.chain.observer.http.api.models

import cats.Monoid
import encry.explorer.core._
import cats.instances.try_._
import scala.util.Try

final case class HttpApiHeader(
  id: Id,
  version: Version,
  parentId: Id,
  payloadId: Id,
  txRoot: TransactionRoot,
  timestamp: Timestamp,
  height: HeaderHeight,
  nonce: Nonce,
  difficulty: Difficulty,
  equihashSolution: List[Int],
  stateRoot: StateRoot
)

object HttpApiHeader {
  object instances {
    implicit object HttpApiHeaderMonoid extends Monoid[HttpApiHeader] {
      override def empty: HttpApiHeader =
        HttpApiHeader(
          Id.fromString[Try]("").get,
          Version(-1: Byte),
          Id.fromString[Try]("").get,
          Id.fromString[Try]("").get,
          TransactionRoot(""),
          Timestamp(-1L),
          HeaderHeight(-1),
          Nonce(-1),
          Difficulty(-1),
          List.empty[Int],
          StateRoot("")
        )

      override def combine(x: HttpApiHeader, y: HttpApiHeader): HttpApiHeader = x
    }
  }
}
