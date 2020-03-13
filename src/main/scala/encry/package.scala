import io.estatico.newtype.macros.newtype

package object encry {

  @newtype final case class Timestamp(long: Long)

  @newtype final case class Id(string: String)

  @newtype final case class HeaderHeight(int: Int)

  @newtype final case class TransactionRoot(string: String)

  @newtype final case class StateRoot(string: String)

  @newtype final case class Version(byte: Byte)

  @newtype final case class Nonce(long: Long)

  @newtype final case class Difficulty(long: Long)

  @newtype final case class TxFee(long: Long)

  @newtype final case class ContractHash(string: String)

  @newtype final case class Amount(long: Long)

  @newtype final case class TokenId(array: Array[Byte])

  @newtype final case class Data(array: Array[Byte])

  @newtype final case class SerializedProofValue(string: String)

  @newtype final case class Address(string: String)

  @newtype final case class InputContract(string: String)

}
