package encry.explorer

import tofu.HasContext

package object env {

  type ContextDB[CI[_], F[_]] = F HasContext DBContext[CI, F]

  type ContextHttpClient[F[_]] = F HasContext HttpClientContext[F]

  type ContextApplication[F[_], CI[_]] = F HasContext AppContext[F, CI]

  type ContextSharedQueues[F[_]] = F HasContext SharedQueuesContext[F]

  type ContextClientQueues[F[_]] = F HasContext HttpClientQueuesContext[F]

  type ContextLogging[F[_]] = F HasContext LoggerContext[F]

}
