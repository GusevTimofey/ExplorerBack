package encry.explorer.chain.observer.programms

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import encry.explorer.chain.observer.http.api.models.HttpApiBlock
import encry.explorer.chain.observer.services.NodeObserver
import encry.explorer.core.{ HeaderHeight, Id, UrlAddress }
import fs2.Stream
import org.http4s.client.Client

trait ChainSynchronizer[F[_]] {
  def run: Stream[F, Unit]
}

object ChainSynchronizer {

  def apply[F[_]: Sync](client: Client[F]): ChainSynchronizer[F] = new ChainSynchronizer[F] {
    override def run: Stream[F, Unit] = ???

    val observerService: F[NodeObserver[F]] =
      for { url <- UrlAddress.fromString[F]("http://172.16.11.14:9051") } yield NodeObserver(client, url)

    def getActualInfo(initHeight: Int): F[Option[HttpApiBlock]] =
      for {
        service <- observerService
        idRaw   <- service.getBestBlockIdAt(HeaderHeight(initHeight))
        id      <- Id.fromString(idRaw.get)
        block   <- service.getBlockBy(id)
      } yield block
  }

}
