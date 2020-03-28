package encry.explorer.chain.observer.services

import cats.Parallel
import cats.effect.concurrent.Ref
import cats.effect.{ Sync, Timer }
import cats.instances.try_._
import cats.syntax.applicative._
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.option._
import cats.syntax.parallel._
import encry.explorer.chain.observer.http.api.models.HttpApiBlock
import encry.explorer.core.{ HeaderHeight, Id, UrlAddress }
import io.chrisdavenport.log4cats.Logger
import org.http4s.client.Client

import scala.util.Try

trait GatheredInfoProcessor[F[_]] {

  def getBestBlockAt(height: HeaderHeight): F[Option[HttpApiBlock]]

  def getFullChainHeight: F[Option[Int]]

  def getHeadersHeight: F[Option[Int]]
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
          idToUrlsOpt <- getAccumulatedBestBlockIdAt(height)
          block <- (for {
                    (idRaw, urls) <- idToUrlsOpt if urls.nonEmpty
                    id            <- Id.fromString[Try](idRaw).toOption
                  } yield id -> urls) match {
                    case Some((id, head :: _)) => observer.getBlockBy(id)(head)
                    case _                     => none[HttpApiBlock].pure[F]
                  }
        } yield block

      override def getFullChainHeight: F[Option[Int]] = extractM(getAccumulatedBestChainFullHeight)

      override def getHeadersHeight: F[Option[Int]] = extractM(getAccumulatedBestChainHeadersHeight)

      private def getAccumulatedBestBlockIdAt(height: HeaderHeight): F[Option[(String, List[UrlAddress])]] =
        requestMany(observer.getBestBlockIdAt(height)).map { computeResult }

      private def getAccumulatedBestChainFullHeight: F[Option[(Int, List[UrlAddress])]] =
        requestMany(observer.getBestFullHeight).map { computeResult }

      private def getAccumulatedBestChainHeadersHeight: F[Option[(Int, List[UrlAddress])]] =
        requestMany(observer.getBestHeadersHeight).map { computeResult }

      private def extractM[J]: F[Option[(J, List[UrlAddress])]] => F[Option[J]] =
        (k: F[Option[(J, List[UrlAddress])]]) =>
          for { kToV <- k } yield
            (for { (k, _) <- kToV } yield k) match {
              case Some(u) => u.some
              case _       => none[J]
          }

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
