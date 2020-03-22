package encry.explorer

import doobie.util.meta.Meta
import encry.explorer.core.refinedTypes._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Url
import eu.timepit.refined.types.string.HexString
import io.circe.{ Decoder, Encoder }
import io.estatico.newtype.macros.newtype

package object core {

  object refinedTypes {

    type UrlAddressType = String Refined Url

  }

  @newtype
  final case class Timestamp(value: Long)
  object Timestamp {
    implicit def meta: Meta[Timestamp]       = deriving
    implicit def encoder: Encoder[Timestamp] = deriving
    implicit def decoder: Decoder[Timestamp] = deriving
  }

  @newtype
  final case class Id(value: HexString) {
    def getValue: String = value.value
  }
  object Id {
    implicit def meta: Meta[Id] = deriving
  }

  @newtype
  final case class HeaderHeight(value: Int)
  object HeaderHeight {
    implicit def meta: Meta[HeaderHeight] = deriving
  }

  @newtype
  final case class TransactionRoot(value: HexString)
  object TransactionRoot {
    implicit def meta: Meta[TransactionRoot] = deriving
  }

  @newtype
  final case class StateRoot(value: HexString)
  object StateRoot {
    implicit def meta: Meta[StateRoot] = deriving
  }

  @newtype
  final case class Version(value: Byte)
  object Version {
    implicit def meta: Meta[Version] = deriving
  }

  @newtype
  final case class Nonce(value: Long)
  object Nonce {
    implicit def meta: Meta[Nonce] = deriving
  }

  @newtype
  final case class Difficulty(value: Long)
  object Difficulty {
    implicit def meta: Meta[Difficulty] = deriving
  }

  @newtype
  final case class TxFee(value: Long)
  object TxFee {
    implicit def meta: Meta[TxFee] = deriving
  }

  @newtype
  final case class ContractHash(value: HexString)
  object ContractHash {
    implicit def meta: Meta[ContractHash] = deriving
  }

  @newtype
  final case class Amount(value: Long)
  object Amount {
    implicit def meta: Meta[Amount] = deriving
  }

  @newtype
  final case class TokenId(value: Array[Byte])
  object TokenId {
    implicit def meta: Meta[TokenId] = deriving
  }

  @newtype
  final case class Data(value: Array[Byte])
  object Data {
    implicit def meta: Meta[Data] = deriving
  }

  @newtype
  final case class SerializedProofValue(value: String)
  object SerializedProofValue {
    implicit def meta: Meta[SerializedProofValue] = deriving
  }

  @newtype
  final case class Address(value: String)
  object Address {
    implicit def meta: Meta[Address] = deriving
  }

  @newtype
  final case class InputContract(value: String)
  object InputContract {
    implicit def meta: Meta[InputContract] = deriving
  }

  @newtype
  final case class TransactionsQuantity(value: Int)
  object TransactionsQuantity {
    implicit def meta: Meta[TransactionsQuantity] = deriving
  }

  @newtype
  final case class UrlAddress(value: UrlAddressType)
}
