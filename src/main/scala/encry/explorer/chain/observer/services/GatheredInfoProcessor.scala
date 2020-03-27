package encry.explorer.chain.observer.services

import cats.Parallel
import cats.effect.concurrent.Ref
import cats.effect.{ Sync, Timer }
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.parallel._
import encry.explorer.chain.observer.http.api.models.HttpApiBlock
import encry.explorer.core.{ HeaderHeight, Id, UrlAddress }
import io.chrisdavenport.log4cats.Logger
import org.http4s.client.Client

trait GatheredInfoProcessor[F[_]] {

  def getBestBlockIdAt(height: HeaderHeight): F[Option[(String, List[UrlAddress])]]

  def getBestChainFullHeight: F[Option[(Int, List[UrlAddress])]]

  def getBestChainHeadersHeight: F[Option[(Int, List[UrlAddress])]]

  def getBestBlockAt(height: HeaderHeight): F[Option[HttpApiBlock]]
}

object GatheredInfoProcessor {

  def apply[F[_]: Sync: Logger: Timer: Parallel](
    ref: Ref[F, List[UrlAddress]],
    client: Client[F],
    observer: NodeObserver[F]
  ): GatheredInfoProcessor[F] =
    new GatheredInfoProcessor[F] {

      //todo: unsafe
      override def getBestBlockAt(height: HeaderHeight): F[Option[HttpApiBlock]] =
        for {
          idToUrlsOpt <- getBestBlockIdAt(height)
          id          <- Id.fromString(idToUrlsOpt.get._1)
          block       <- observer.getBlockBy(id, idToUrlsOpt.get._2.head)
        } yield block

      override def getBestBlockIdAt(height: HeaderHeight): F[Option[(String, List[UrlAddress])]] =
        gatherIdsInformationAt(height).map { elems =>
          Either.catchNonFatal {
            val (id, urlsRaw) = elems.groupBy(_._2).maxBy(_._2.size)
            id -> urlsRaw.map(_._1)
          }.toOption
        }

      override def getBestChainFullHeight: F[Option[(Int, List[UrlAddress])]] =
        gatherBestChainFullHeight.map { elems =>
          Either.catchNonFatal {
            val (height, urlsRaw) = elems.groupBy(_._2).maxBy(_._2.size)
            height -> urlsRaw.map(_._1)
          }.toOption
        }

      override def getBestChainHeadersHeight: F[Option[(Int, List[UrlAddress])]] =
        gatherBestChainHeadersHeight.map { elems =>
          Either.catchNonFatal {
            val (height, urlsRaw) = elems.groupBy(_._2).maxBy(_._2.size)
            height -> urlsRaw.map(_._1)
          }.toOption
        }

      private def gatherIdsInformationAt(height: HeaderHeight): F[List[(UrlAddress, String)]] =
        ref.get.flatMap { urls =>
          val toPerform = urls.map(url => observer.getBestBlockIdAt(height, url).map { _.map { url -> _ } })
          computeInParallel(toPerform).map { _.flatten }
        }

      private def gatherBestChainFullHeight: F[List[(UrlAddress, Int)]] =
        ref.get.flatMap { urls =>
          val toPerform = urls.map(url => observer.getBestFullHeight(url).map { _.map { url -> _ } })
          computeInParallel(toPerform) map { _.flatten }
        }

      private def gatherBestChainHeadersHeight: F[List[(UrlAddress, Int)]] =
        ref.get.flatMap { urls =>
          val toPerform = urls.map(url => observer.getBestHeadersHeight(url).map { _.map { url -> _ } })
          computeInParallel(toPerform).map { _.flatten }
        }

      private def computeInParallel[R]: List[F[R]] => F[List[R]] =
        (f: List[F[R]]) => { import cats.instances.list._; f.parSequence }

    }

}
