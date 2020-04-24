package encry.explorer

import tofu.HasContext

package object env {

  type HasCoreContext[F[_], CI[_]] = F HasContext CoreContext[F, CI]

  type HasExplorerContext[F[_]] = F HasContext ExplorerContext[F]

  type HasHttpApiContext[F[_]] = F HasContext HttpClientContext[F]
}
