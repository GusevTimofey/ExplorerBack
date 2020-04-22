package encry.explorer.core.db.models

import encry.explorer.chain.observer.http.api.models.HttpApiTransaction
import encry.explorer.chain.observer.http.api.models.boxes.HttpApiBox
import encry.explorer.chain.observer.http.api.models.boxes.HttpApiBox.{
  HttpApiAssetBox,
  HttpApiDataBox,
  HttpApiTokenIssuingBox
}
import encry.explorer.core._

final case class OutputDBModel(
  id: Id,
  header_id: Id,
  transactionId: Id,
  outputTypeId: Byte,
  contractHash: ContractHash,
  isActive: Boolean,
  nonce: Nonce,
  amount: Amount,
  data: String,
  tokenId: String
)

object OutputDBModel {
  def fromOutput(txId: Id, output: HttpApiBox, headerId: Id): OutputDBModel =
    output match {
      case HttpApiAssetBox(outputType, id, proposition, nonce, value, tokenId) =>
        OutputDBModel(
          id,
          headerId,
          txId,
          outputType.value,
          proposition.contractHash,
          isActive = true,
          nonce,
          value,
          "",
          tokenId.map(_.value).getOrElse("")
        )
      case HttpApiDataBox(outputType, id, proposition, nonce, data) =>
        OutputDBModel(
          id,
          headerId,
          txId,
          outputType.value,
          proposition.contractHash,
          isActive = true,
          nonce,
          Amount(0),
          data.value,
          ""
        )
      case HttpApiTokenIssuingBox(outputType, id, tokenId, proposition, nonce, amount) =>
        OutputDBModel(
          id,
          headerId,
          txId,
          outputType.value,
          proposition.contractHash,
          isActive = true,
          nonce,
          amount,
          "",
          tokenId.value
        )
    }

  def fromHttpApi(inputTransaction: HttpApiTransaction, headerId: Id): List[OutputDBModel] =
    inputTransaction.outputs.map(output => fromOutput(inputTransaction.id, output, headerId))
}
