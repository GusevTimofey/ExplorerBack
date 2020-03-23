package encry.explorer.chain.observer.programms

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import encry.explorer.chain.observer.services.NodeObserver
import encry.explorer.core.{ HeaderHeight, Id, UrlAddress }
import eu.timepit.refined.types.string.HexString
import fs2.Stream
import org.http4s.client.Client

trait ChainSynchronizer[F[_]] {
  def run: Stream[F, Unit]
}

object ChainSynchronizer {

  def apply[F[_]: Sync](client: Client[F]): ChainSynchronizer[F] = new ChainSynchronizer[F] {
    override def run: Stream[F, Unit] = ???

    val observerService: NodeObserver[F] = NodeObserver(client, UrlAddress("http://172.16.11.14:9051"))

    def getActualInfo(initHeight: Int) =
      for {
        id    <- observerService.getBestBlockIdAt(HeaderHeight(initHeight))
        block <- observerService.getBlockBy(Id(id.get))
      } yield ()
  }

}
