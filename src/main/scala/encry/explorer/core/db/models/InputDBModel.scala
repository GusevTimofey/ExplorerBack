package encry.explorer.core.db.models

import encry.explorer.chain.observer.http.api.models.HttpApiTransaction
import encry.explorer.core._

final case class InputDBModel(
  transactionId: Id,
  header_id: Id,
  boxId: Id,
  proofs: String,
  contract: String,
)

object InputDBModel {
  def fromHttpApi(inputTransaction: HttpApiTransaction, headerId: Id): List[InputDBModel] =
    inputTransaction.inputs.map { input =>
      InputDBModel(
        inputTransaction.id,
        headerId,
        input.boxId,
        input.proofs.mkString(", "),
        input.contract.value.value
      )
    }
}