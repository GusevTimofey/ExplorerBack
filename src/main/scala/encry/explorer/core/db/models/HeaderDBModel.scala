package encry.explorer.core.db.models

import encry.explorer.chain.observer.http.api.models.HttpApiBlock
import encry.explorer.chain.observer.http.api.models.boxes.HttpApiBox.HttpApiAssetBox
import encry.explorer.core._

final case class HeaderDBModel(
  id: Id,
  parentId: Id,
  transactionsRoot: TransactionRoot,
  stateRoot: StateRoot,
  version: Version,
  height: HeaderHeight,
  difficulty: Difficulty,
  timestamp: Timestamp,
  nonce: Nonce,
  equihashSolution: List[Int],
  transactionQuantity: TransactionsQuantity,
  minerAddress: ContractHash,
  isInBestChain: Boolean
)

object HeaderDBModel {
  def fromHttpApi(inputBlock: HttpApiBlock): HeaderDBModel =
    HeaderDBModel(
      inputBlock.header.id,
      inputBlock.header.parentId,
      inputBlock.header.txRoot,
      inputBlock.header.stateRoot,
      inputBlock.header.version,
      inputBlock.header.height,
      inputBlock.header.difficulty,
      inputBlock.header.timestamp,
      inputBlock.header.nonce,
      inputBlock.header.equihashSolution,
      TransactionsQuantity(inputBlock.payload.transactions.size),
      inputBlock.payload.transactions.last.outputs.collect { case ab: HttpApiAssetBox => ab }.last.proposition.contractHash,
      isInBestChain = true
    )
}
