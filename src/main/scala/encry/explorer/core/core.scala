package encry.explorer

import cats.{ Applicative, ApplicativeError }
import doobie.util.meta.Meta
import encry.explorer.core.refinedInstances.{ Base16, UrlString }
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.{ HexStringSpec, Url }
import io.circe.{ Decoder, Encoder }
import eu.timepit.refined.refineV
import cats.syntax.applicative._
import io.estatico.newtype.macros.newtype
//todo These imports have to be declared in the scope. Doesn't compile without it. Intellij IDEA bug.
//todo import doobie.refined.implicits._; import io.circe.refined._
import doobie.refined.implicits._
import io.circe.refined._

package object core {

  object refinedInstances {

    type UrlString = String Refined Url

    type Base16 = String Refined HexStringSpec
  }

  @newtype final case class Timestamp(value: Long)
  object Timestamp {
    implicit def meta: Meta[Timestamp]       = deriving
    implicit def encoder: Encoder[Timestamp] = deriving
    implicit def decoder: Decoder[Timestamp] = deriving
  }

  @newtype final case class Id(value: Base16) { def getValue: String = value.value }
  object Id {
    implicit def meta: Meta[Id]       = deriving
    implicit def encoder: Encoder[Id] = deriving
    implicit def decoder: Decoder[Id] = deriving

    def fromString[F[_]: Applicative: ApplicativeError[*[_], Throwable]](string: String): F[Id] =
      refineV[HexStringSpec](string) match {
        case Left(err) =>
          ApplicativeError[F, Throwable].raiseError(new Throwable(s"Incorrect nested hex if data data cause: $err"))
        case Right(value) => Id.apply(value).pure[F]
      }
  }

  @newtype final case class HeaderHeight(value: Int)
  object HeaderHeight {
    implicit def meta: Meta[HeaderHeight]       = deriving
    implicit def encoder: Encoder[HeaderHeight] = deriving
    implicit def decoder: Decoder[HeaderHeight] = deriving
  }

  @newtype final case class TransactionRoot(value: String)
  object TransactionRoot {
    implicit def meta: Meta[TransactionRoot]       = deriving
    implicit def encoder: Encoder[TransactionRoot] = deriving
    implicit def decoder: Decoder[TransactionRoot] = deriving

//    def fromString[F[_]: Applicative: ApplicativeError[*[_], Throwable]](string: String): F[TransactionRoot] =
//      refineV[HexStringSpec](string) match {
//        case Left(err) =>
//          ApplicativeError[F, Throwable].raiseError(new Throwable(s"Incorrect nested hex if data data cause: $err"))
//        case Right(value) => TransactionRoot.apply(value).pure[F]
//      }
  }

  @newtype final case class StateRoot(value: String)
  object StateRoot {
    implicit def meta: Meta[StateRoot]       = deriving
    implicit def encoder: Encoder[StateRoot] = deriving
    implicit def decoder: Decoder[StateRoot] = deriving

//    def fromString[F[_]: Applicative: ApplicativeError[*[_], Throwable]](string: String): F[StateRoot] =
//      refineV[HexStringSpec](string) match {
//        case Left(err) =>
//          ApplicativeError[F, Throwable].raiseError(new Throwable(s"Incorrect nested hex if data data cause: $err"))
//        case Right(value) => StateRoot.apply(value).pure[F]
//      }
  }

  @newtype final case class Version(value: Byte)
  object Version {
    implicit def meta: Meta[Version]       = deriving
    implicit def encoder: Encoder[Version] = deriving
    implicit def decoder: Decoder[Version] = deriving
  }

  @newtype final case class Nonce(value: Long)
  object Nonce {
    implicit def meta: Meta[Nonce]       = deriving
    implicit def encoder: Encoder[Nonce] = deriving
    implicit def decoder: Decoder[Nonce] = deriving
  }

  @newtype final case class Difficulty(value: Long)
  object Difficulty {
    implicit def meta: Meta[Difficulty]       = deriving
    implicit def encoder: Encoder[Difficulty] = deriving
    implicit def decoder: Decoder[Difficulty] = deriving
  }

  @newtype final case class TxFee(value: Long)
  object TxFee { implicit def meta: Meta[TxFee] = deriving }

  @newtype final case class ContractHash(value: String) { def getValue: String = value }
  object ContractHash {
    implicit def meta: Meta[ContractHash]       = deriving
    implicit def encoder: Encoder[ContractHash] = deriving
    implicit def decoder: Decoder[ContractHash] = deriving
  }

  @newtype final case class Amount(value: Long)
  object Amount {
    implicit def meta: Meta[Amount]       = deriving
    implicit def encoder: Encoder[Amount] = deriving
    implicit def decoder: Decoder[Amount] = deriving
  }

  @newtype final case class TokenId(value: Array[Byte])
  object TokenId {
    implicit def meta: Meta[TokenId]       = deriving
    implicit def encoder: Encoder[TokenId] = deriving
    implicit def decoder: Decoder[TokenId] = deriving
  }

  @newtype final case class Data(value: Array[Byte])
  object Data {
    implicit def meta: Meta[Data]       = deriving
    implicit def encoder: Encoder[Data] = deriving
    implicit def decoder: Decoder[Data] = deriving
  }

  @newtype final case class SerializedProofValue(value: String)
  object SerializedProofValue { implicit def meta: Meta[SerializedProofValue] = deriving }

  @newtype final case class Address(value: String)
  object Address {
    implicit def meta: Meta[Address]       = deriving
    implicit def encoder: Encoder[Address] = deriving
    implicit def decoder: Decoder[Address] = deriving
  }

  @newtype final case class InputContract(value: Base16)
  object InputContract { implicit def meta: Meta[InputContract] = deriving }

  @newtype final case class TransactionsQuantity(value: Int)
  object TransactionsQuantity { implicit def meta: Meta[TransactionsQuantity] = deriving }

  @newtype final case class UrlAddress(value: UrlString)
  object UrlAddress {
    def fromString[F[_]: Applicative: ApplicativeError[*[_], Throwable]](string: String): F[UrlAddress] =
      refineV[Url](string) match {
        case Left(err) =>
          ApplicativeError[F, Throwable].raiseError(new Throwable(s"Incorrect nested url data cause: $err"))
        case Right(value) => UrlAddress.apply(value).pure[F]
      }
  }
}
