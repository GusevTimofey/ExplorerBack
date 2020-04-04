package encry.explorer.chain.observer.programs

import encry.explorer.chain.observer.services.{ ClientService, GatheringService }
import fs2.Stream

trait ForkDownloader[F[_]] {
  def run: Stream[F, Unit]
}

//todo not finished yet
object ForkDownloader {
  def apply[F[_]](
    gatheringService: GatheringService[F],
    clientService: ClientService[F],
  ): ForkDownloader[F] = new ForkDownloader[F] {
    override def run: Stream[F, Unit] = Stream(()).covary[F]
  }
}
