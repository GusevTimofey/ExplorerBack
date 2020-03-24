package encry.explorer.chain.observer.programms

import cats.effect.{ Concurrent, ConcurrentEffect, Sync, Timer }
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import encry.explorer.chain.observer.http.api.models.HttpApiBlock
import encry.explorer.chain.observer.services.NodeObserver
import encry.explorer.core.{ HeaderHeight, Id, UrlAddress }
import fs2.Stream
import fs2.concurrent.Queue
import io.chrisdavenport.log4cats.Logger
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

trait ChainSynchronizer[F[_]] {
  def run: Stream[F, Unit]
}

object ChainSynchronizer {

  def apply[F[_]: Sync: Timer: Concurrent: Logger: ConcurrentEffect](client: Client[F]): F[ChainSynchronizer[F]] =
    for {
      queue <- Queue.bounded[F, HttpApiBlock](100)
    } yield
      new ChainSynchronizer[F] {
        override def run: Stream[F, Unit] =
          Stream.eval(getActualInfo(0)) concurrently Stream
            .emit(())
            .covary[F]
            .metered(0.5.seconds)
            .evalMap(_ => queue.dequeue1)

        val observerService: F[NodeObserver[F]] =
          for {
            _   <- Logger[F].info(s"Create observer service step 1")
            url <- UrlAddress.fromString[F]("http://172.16.11.14:9051")
            _   <- Logger[F].info(s"Create observer service step 2")
            res <- NodeObserver(client, url).pure[F]
            _   <- Logger[F].info(s"Create observer service step 3")
          } yield res

        def getActualInfo(initHeight: Int): F[Unit] =
          for {
            _       <- Logger[F].info(s"Run observer step 1")
            service <- observerService
            _       <- Logger[F].info(s"Run observer step 2")
            idRaw   <- service.getBestBlockIdAt(HeaderHeight(initHeight))
            _       <- Logger[F].info(s"Run observer step 3")
            id      <- Id.fromString(idRaw.get)
            _       <- Logger[F].info(s"CRun observer step 4")
            block   <- service.getBlockBy(id)
            _       <- Logger[F].info(s"Got $block from http api.")
            _       <- queue.enqueue1(block.get)
            _       <- Timer[F].sleep(1.seconds)
          } yield getActualInfo(initHeight + 1)

      }

}
