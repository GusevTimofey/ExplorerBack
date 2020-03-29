package encry.explorer.chain.observer.services

import cats.Parallel
import cats.effect.concurrent.Ref
import cats.effect.{ Sync, Timer }
import cats.instances.list._
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

  def getIdsInRollbackRange(startsFrom: Int, rollbackRange: Int): F[List[String]]
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
                    (idRaw, urls) <- idToUrlsOpt
                    id            <- Id.fromString[Try](idRaw).toOption
                  } yield id -> urls) match {
                    case Some((id, rl @ _ :: _)) => tryToRichExpectedElement(observer.getBlockBy(id), rl)
                    case _                       => none[HttpApiBlock].pure[F]
                  }
        } yield block

      override def getFullChainHeight: F[Option[Int]] = extractM(getAccumulatedBestChainFullHeight)

      override def getHeadersHeight: F[Option[Int]] = extractM(getAccumulatedBestChainHeadersHeight)

      def getIdsInRollbackRange(startsFrom: Int, rollbackRange: Int): F[List[String]] =
        extractM(getIdsFromMany(startsFrom, rollbackRange)).map {
          case Some(elements) => elements
          case _              => List.empty[String]
        }

      private def getIdsFromMany(startsFrom: Int, rollbackRange: Int): F[Option[(List[String], List[UrlAddress])]] =
        requestManyPar[List, String](observer.getLastIds(rollbackRange, startsFrom)).map {
          _.collect { case (address, value @ _ :: _) => address -> value }
        }.map { computeResult }

      private def getAccumulatedBestBlockIdAt(height: HeaderHeight): F[Option[(String, List[UrlAddress])]] =
        requestManyPar[Option, String](observer.getBestBlockIdAt(height)).map {
          _.collect { case (address, Some(value)) => address -> value }
        }.map { computeResult }

      private def getAccumulatedBestChainFullHeight: F[Option[(Int, List[UrlAddress])]] =
        requestManyPar[Option, Int](observer.getBestFullHeight).map {
          _.collect { case (address, Some(value)) => address -> value }
        }.map { computeResult }

      private def getAccumulatedBestChainHeadersHeight: F[Option[(Int, List[UrlAddress])]] =
        requestManyPar[Option, Int](observer.getBestHeadersHeight).map {
          _.collect { case (address, Some(value)) => address -> value }
        }.map { computeResult }

      private def extractM[J]: F[Option[(J, List[UrlAddress])]] => F[Option[J]] =
        (k: F[Option[(J, List[UrlAddress])]]) =>
          for { kToV <- k } yield
            (for { (k, _) <- kToV } yield k) match {
              case Some(u) => u.some
              case _       => none[J]
          }

      private def requestManyPar[T[_], R]: (UrlAddress => F[T[R]]) => F[List[(UrlAddress, T[R])]] =
        (f: UrlAddress => F[T[R]]) => ref.get.flatMap(_.map(url => f(url).map(url -> _)).parSequence)

      private def computeResult[D]: List[(UrlAddress, D)] => Option[(D, List[UrlAddress])] =
        (inputs: List[(UrlAddress, D)]) =>
          Either.catchNonFatal {
            val (dRes, urlsRaw) = inputs.groupBy(_._2).maxBy(_._2.size)
            dRes -> urlsRaw.map(_._1)
          }.toOption

      private def tryToRichExpectedElement[U]: (UrlAddress => F[Option[U]], List[UrlAddress]) => F[Option[U]] = {
        (f: UrlAddress => F[Option[U]], urls: List[UrlAddress]) =>
          def loop(urls: List[UrlAddress]): F[Option[U]] =
            urls.headOption match {
              case Some(url) =>
                f(url).flatMap {
                  case Some(potentialElement) => potentialElement.some.pure[F]
                  case _                      => loop(urls.drop(1))
                }
              case None => none[U].pure[F]
            }

          loop(urls)
      }
    }

}
