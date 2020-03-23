package encry.explorer.chain

import eu.timepit.refined.types.string.HexString
import io.estatico.newtype.macros.newtype

package object observer {

  @newtype final case class Contract(value: HexString)

  @newtype final case class ProofValue(value: HexString)

  @newtype final case class TypeId(value: Byte)

}
