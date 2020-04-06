package encry.explorer.chain

import encry.explorer.core.UrlAddress
import eu.timepit.refined.types.string.HexString
import io.circe.{ Decoder, Encoder }
import io.estatico.newtype.macros.newtype
//todo This import has to be declared in the scope. Doesn't compile without it. Intellij IDEA bug.
//todo import io.circe.refined._
import io.circe.refined._

package object observer {

  @newtype final case class Contract(value: HexString)
  object Contract {
    implicit def encoder: Encoder[Contract] = deriving
    implicit def decoder: Decoder[Contract] = deriving
  }

  @newtype final case class ProofValue(value: HexString)
  object ProofValue {
    implicit def encoder: Encoder[ProofValue] = deriving
    implicit def decoder: Decoder[ProofValue] = deriving
  }

  @newtype final case class TypeId(value: Byte)
  object TypeId {
    implicit def encoder: Encoder[TypeId] = deriving
    implicit def decoder: Decoder[TypeId] = deriving
  }

  @newtype final case class BanTime(value: Long)

  object errors {
    sealed trait HttpApiErr
    final case object NoSuchElementErr     extends HttpApiErr
    final case object AddressIsUnreachable extends HttpApiErr
  }

}
