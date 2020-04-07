package encry.explorer.chain.observer.programs

import cats.effect.Sync
import cats.effect.concurrent.Ref
import encry.explorer.chain.observer.http.api.models.HttpApiBlock
import encry.explorer.chain.observer.services.{ ClientService, GatheringService }
import fs2.Stream
import fs2.concurrent.Queue
import cats.syntax.functor._

trait ForkDownloader[F[_]] {
  def run: Stream[F, Unit]
}

//todo implement
object ForkDownloader {
  def apply[F[_]: Sync](
    gatheringService: GatheringService[F],
    clientService: ClientService[F],
    isChainSyncedRef: Ref[F, Boolean],
    outgoingForksBlocks: Queue[F, HttpApiBlock]
  ): F[ForkDownloader[F]] =
    for {
      lastRollBackBlocks <- Ref.of[F, Map[Int, List[String]]](Map.empty)
    } yield
      new ForkDownloader[F] {
        override def run: Stream[F, Unit] = Stream(()).covary[F]
      }
}
