package encry.explorer.core.db.models

import encry.explorer.chain.observer.http.api.models.HttpApiTransaction
import encry.explorer.chain.observer.http.api.models.boxes.{
  HttpApiAssetBox,
  HttpApiBox,
  HttpApiDataBox,
  HttpApiTokenIssuingBox
}
import encry.explorer.core._

final case class OutputDBModel(
  id: Id,
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
  def fromOutput(txId: Id, output: HttpApiBox): OutputDBModel =
    output match {
      case HttpApiAssetBox(outputType, id, proposition, nonce, value, tokenId) =>
        OutputDBModel(
          id,
          txId,
          outputType.value,
          proposition.contractHash,
          isActive = true,
          nonce,
          value,
          "",
          tokenId.map(_.value.toString).getOrElse("")
        )
      case HttpApiDataBox(outputType, id, proposition, nonce, data) =>
        OutputDBModel(
          id,
          txId,
          outputType.value,
          proposition.contractHash,
          isActive = true,
          nonce,
          Amount(0),
          data.value.toString,
          ""
        )
      case HttpApiTokenIssuingBox(outputType, id, proposition, nonce, amount, tokenId) =>
        OutputDBModel(
          id,
          txId,
          outputType.value,
          proposition.contractHash,
          isActive = true,
          nonce,
          amount,
          "",
          tokenId.value.toString
        )
    }

  def fromHttpApi(inputTransaction: HttpApiTransaction): List[OutputDBModel] =
    inputTransaction.outputs.map { output =>
      fromOutput(inputTransaction.id, output)
    }
}
