package encry.explorer.core.db.models

import encry.explorer.chain.observer.http.api.models.HttpApiBlock
import encry.explorer.core._

final case class TransactionDBModel(
  id: Id,
  headerId: Id,
  fee: TxFee,
  timestamp: Timestamp,
  proof: String,
  isCoinbaseTransaction: Boolean
)

object TransactionDBModel {

  def fromHttpApi(inputBlock: HttpApiBlock): List[TransactionDBModel] =
    inputBlock.payload.transactions.map { tx =>
      TransactionDBModel(
        tx.id,
        inputBlock.header.id,
        TxFee(tx.fee.value),
        tx.timestamp,
        tx.defaultProofOpt.map(_.toString).getOrElse(""),
        isCoinbaseTransaction = false //todo incorrect
      )
    }
}
