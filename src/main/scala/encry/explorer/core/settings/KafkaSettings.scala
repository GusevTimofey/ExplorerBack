package encry.explorer.core.settings

final case class KafkaSettings(
  consumerAddress: String,
  concurrencySize: Int,
  chunkSize: Int
)
