package encry.explorer.env

import cats.effect.Concurrent
import cats.syntax.flatMap._
import cats.syntax.functor._
import encry.explorer.chain.observer.programs.UrlsManager.UrlCurrentState
import encry.explorer.core.UrlAddress
import encry.explorer.core.settings.ExplorerSettings
import fs2.concurrent.Queue

case class HttpClientQueuesContext[F[_]](
  unreachableUrlsQueue: Queue[F, UrlAddress],
  urlStatisticQueue: Queue[F, UrlCurrentState]
)

object HttpClientQueuesContext {
  def create[F[_]: Concurrent](ES: ExplorerSettings): F[HttpClientQueuesContext[F]] =
    for {
      unreachableUrlsQueue <- Queue.bounded[F, UrlAddress](ES.httpClientSettings.maxConnections * 2)
      urlStatisticQueue    <- Queue.bounded[F, UrlCurrentState](ES.httpClientSettings.maxConnections * 2)
    } yield HttpClientQueuesContext(unreachableUrlsQueue, urlStatisticQueue)
}
