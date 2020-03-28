package encry.explorer.chain.observer.services

import cats.Parallel
import cats.effect.concurrent.Ref
import cats.effect.{ Sync, Timer }
import cats.syntax.either._
import cats.syntax.option._
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.parallel._
import cats.instances.try_._
import encry.explorer.chain.observer.http.api.models.HttpApiBlock
import encry.explorer.core.{ HeaderHeight, Id, UrlAddress }
import io.chrisdavenport.log4cats.Logger
import org.http4s.client.Client

import scala.util.Try

trait GatheredInfoProcessor[F[_]] {

  def getBestBlockAt(height: HeaderHeight): F[Option[HttpApiBlock]]
}

object GatheredInfoProcessor {

  def apply[F[_]: Sync: Logger: Timer: Parallel](
    ref: Ref[F, List[UrlAddress]],
    client: Client[F],
    observer: NodeObserver[F]
  ): GatheredInfoProcessor[F] =
    new GatheredInfoProcessor[F] {

      override def getBestBlockAt(height: HeaderHeight): F[Option[HttpApiBlock]] =
        for {
          idToUrlsOpt <- getBestBlockIdAt(height)
          block <- (for {
                    (idRaw, urls) <- idToUrlsOpt if urls.nonEmpty
                    id            <- Id.fromString[Try](idRaw).toOption
                  } yield id -> urls) match {
                    case Some((id, head :: _)) => observer.getBlockBy(id)(head)
                    case _                     => none[HttpApiBlock].pure[F]
                  }
        } yield block

      private def getBestBlockIdAt(height: HeaderHeight): F[Option[(String, List[UrlAddress])]] =
        requestMany(observer.getBestBlockIdAt(height)).map { computeResult }

      private def getBestChainFullHeight: F[Option[(Int, List[UrlAddress])]] =
        requestMany(observer.getBestFullHeight).map { computeResult }

      private def getBestChainHeadersHeight: F[Option[(Int, List[UrlAddress])]] =
        requestMany(observer.getBestHeadersHeight).map { computeResult }

      private def requestMany[R]: (UrlAddress => F[Option[R]]) => F[List[(UrlAddress, R)]] =
        (f: UrlAddress => F[Option[R]]) =>
          ref.get.flatMap { urls =>
            val toPerform = urls.map(url => f(url).map { _.map { url -> _ } })
            computeInParallel(toPerform).map { _.flatten }
        }

      private def computeInParallel[R]: List[F[R]] => F[List[R]] =
        (f: List[F[R]]) => { import cats.instances.list._; f.parSequence }

      private def computeResult[D]: List[(UrlAddress, D)] => Option[(D, List[UrlAddress])] =
        (inputs: List[(UrlAddress, D)]) =>
          Either.catchNonFatal {
            val (dRes, urlsRaw) = inputs.groupBy(_._2).maxBy(_._2.size)
            dRes -> urlsRaw.map(_._1)
          }.toOption

    }

}
