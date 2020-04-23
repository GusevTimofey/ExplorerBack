package encry.explorer.env

import encry.explorer.core.db.algebra.LiftConnectionIO
import encry.explorer.core.db.repositories.{
  HeaderRepository,
  InputRepository,
  OutputRepository,
  TransactionRepository
}

final case class RepositoriesContext[CI[_]](
  hr: HeaderRepository[CI],
  ir: InputRepository[CI],
  or: OutputRepository[CI],
  tr: TransactionRepository[CI]
)

object RepositoriesContext {
  def create[CI[_]: LiftConnectionIO]: RepositoriesContext[CI] =
    RepositoriesContext(HeaderRepository[CI], InputRepository[CI], OutputRepository[CI], TransactionRepository[CI])
}
