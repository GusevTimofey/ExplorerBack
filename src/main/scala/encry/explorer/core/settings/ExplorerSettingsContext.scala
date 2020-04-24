package encry.explorer.core.settings

final case class ExplorerSettingsContext(
  dbSettings: DBSettings,
  httpClientSettings: ChainObserverSettings,
  encrySettings: EncrySettings,
  kafkaSettings: KafkaSettings
)
