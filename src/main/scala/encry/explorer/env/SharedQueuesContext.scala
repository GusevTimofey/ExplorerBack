package encry.explorer.env

import cats.effect.Concurrent
import encry.explorer.chain.observer.http.api.models.HttpApiBlock
import encry.explorer.core.settings.ExplorerSettings
import encry.explorer.events.processing.ExplorerEvent
import fs2.concurrent.Queue
import cats.syntax.flatMap._
import cats.syntax.functor._

final case class SharedQueuesContext[F[_]](
  bestChainBlocks: Queue[F, HttpApiBlock],
  forkBlocks: Queue[F, String],
  eventsQueue: Queue[F, ExplorerEvent]
)

object SharedQueuesContext {
  def create[F[_]: Concurrent](sr: ExplorerSettings): F[SharedQueuesContext[F]] =
    for {
      bestChainBlocks <- Queue.bounded[F, HttpApiBlock](sr.encrySettings.rollbackMaxHeight * 2)
      forkBlocks      <- Queue.bounded[F, String](sr.encrySettings.rollbackMaxHeight)
      eventsQueue     <- Queue.unbounded[F, ExplorerEvent]
    } yield SharedQueuesContext(bestChainBlocks, forkBlocks, eventsQueue)
}
