package encry.explorer.core.settings

final case class ExplorerSettings(
  dbSettings: DBSettings,
  httpClientSettings: ChainObserverSettings,
  encrySettings: EncrySettings
)
