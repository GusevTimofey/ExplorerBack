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
import cats.syntax.traverse._
import encry.explorer.chain.observer.errors._
import encry.explorer.chain.observer.http.api.models.HttpApiBlock
import encry.explorer.core.{ HeaderHeight, Id, UrlAddress }
import io.chrisdavenport.log4cats.Logger
import org.http4s.client.Client

import scala.util.{ Failure, Success, Try }

trait GatheredInfoProcessor[F[_]] {

  def getBlockById(id: String): F[(Option[HttpApiBlock], List[UrlAddress])]

  def getBestBlockAt(height: HeaderHeight): F[(Option[HttpApiBlock], List[UrlAddress])]

  def getFullChainHeight: F[(Option[Int], List[UrlAddress])]

  def getHeadersHeight: F[(Option[Int], List[UrlAddress])]

  def getIdsInRollbackRange(startsFrom: Int, rollbackRange: Int): F[(List[String], List[UrlAddress])]

  def getBlocksByIdsMany(ids: List[String]): F[(List[HttpApiBlock], List[UrlAddress])]
}

object GatheredInfoProcessor {

  def apply[F[_]: Sync: Logger: Timer: Parallel](
    ref: Ref[F, List[UrlAddress]],
    client: Client[F],
    observer: NodeObserver[F]
  ): GatheredInfoProcessor[F] =
    new GatheredInfoProcessor[F] {

      override def getBlockById(id: String): F[(Option[HttpApiBlock], List[UrlAddress])] =
        (for {
          urls <- ref.get
          block <- Id.fromString[Try](id) match {
                    case Failure(_)     => (none[HttpApiBlock] -> List.empty[UrlAddress]).pure[F]
                    case Success(value) => tryToRichExpectedElement[HttpApiBlock](observer.getBlockBy(value), urls)
                  }
        } yield block).flatTap {
          case (block, urls) =>
            Logger[F].info(
              s"Request for block with id: $id was finished. " +
                s"Does such a block exist: ${block.isDefined}. " +
                s"Inconsistent urls are: ${urls.mkString(",")}."
            )
        }

      override def getBestBlockAt(height: HeaderHeight): F[(Option[HttpApiBlock], List[UrlAddress])] =
        for {
          (badUrl, idToUrlsOpt) <- getAccumulatedBestBlockIdAt(height)
          _                     <- Logger[F].info(s"Best block id at height $height is: ${idToUrlsOpt.map(_._1)}.")
          (block, moreBadUrls) <- (for {
                    (idRaw, urls) <- idToUrlsOpt
                    id            <- Id.fromString[Try](idRaw).toOption
                  } yield id -> urls) match {
                    case Some((id, rl @ _ :: _)) => tryToRichExpectedElement(observer.getBlockBy(id), rl)
                    case _                       => (none[HttpApiBlock] -> List.empty[UrlAddress]).pure[F]
                  }
        } yield block -> (badUrl ::: moreBadUrls).distinct

      override def getBlocksByIdsMany(ids: List[String]): F[(List[HttpApiBlock], List[UrlAddress])] =
        ids
          .traverse(getBlockById)
          .map { receivedResponses =>
            @scala.annotation.tailrec
            def loop(
              badUrls: Set[UrlAddress],
              acc: List[HttpApiBlock],
              input: List[(Option[HttpApiBlock], List[UrlAddress])]
            ): (List[HttpApiBlock], List[UrlAddress]) = input.headOption match {
              case Some((Some(response), urls)) => loop(badUrls ++ urls.toSet, response :: acc, input.drop(1))
              case Some((_, urls))              => loop(badUrls ++ urls.toSet, acc, input.drop(1))
              case None                         => acc -> badUrls.toList
            }
            loop(Set.empty, List.empty, receivedResponses)
          }
          .flatTap {
            case (receivedBlocks, _) =>
              Logger[F].info(
                s"Request for ${ids.size} was finished. Received blocks number is: ${receivedBlocks.size}."
              )
          }

      override def getFullChainHeight: F[(Option[Int], List[UrlAddress])] =
        getAccumulatedBestChainFullHeight.map { case (addresses, maybeTuple) => extractM(maybeTuple) -> addresses }

      override def getHeadersHeight: F[(Option[Int], List[UrlAddress])] =
        getAccumulatedBestChainHeadersHeight.map { case (addresses, maybeTuple) => extractM(maybeTuple) -> addresses }

      override def getIdsInRollbackRange(startsFrom: Int, rollbackRange: Int): F[(List[String], List[UrlAddress])] =
        getIdsFromMany(startsFrom, rollbackRange).map {
          case (addresses, maybeTuple) =>
            (extractM(maybeTuple) match {
              case Some(elements) => elements
              case _              => List.empty[String]
            }) -> addresses
        }

      private def getIdsFromMany(
        startsFrom: Int,
        rollbackRange: Int
      ): F[(List[UrlAddress], Option[(List[String], List[UrlAddress])])] =
        handleResponses(requestManyPar(observer.getLastIds(startsFrom, rollbackRange)))

      private def getAccumulatedBestBlockIdAt(
        height: HeaderHeight
      ): F[(List[UrlAddress], Option[(String, List[UrlAddress])])] =
        handleResponses(requestManyPar(observer.getBestBlockIdAt(height)))

      private def getAccumulatedBestChainFullHeight: F[(List[UrlAddress], Option[(Int, List[UrlAddress])])] =
        handleResponses(requestManyPar(observer.getBestFullHeight))

      private def getAccumulatedBestChainHeadersHeight: F[(List[UrlAddress], Option[(Int, List[UrlAddress])])] =
        handleResponses(requestManyPar(observer.getBestHeadersHeight))

      private def handleResponses[R](
        i: F[List[(UrlAddress, Either[HttpApiErr, R])]]
      ): F[(List[UrlAddress], Option[(R, List[UrlAddress])])] =
        i.map { filterResponses }.map { case (errUrl, fr) => errUrl -> computeResult(fr) }

      private def filterResponses[R](
        elems: List[(UrlAddress, Either[HttpApiErr, R])]
      ): (List[UrlAddress], List[(UrlAddress, R)]) =
        elems.foldLeft(List.empty[UrlAddress], List.empty[(UrlAddress, R)]) {
          case ((badUrls, responses), (url, Right(response)))      => badUrls          -> ((url -> response) :: responses)
          case ((badUrls, responses), (_, Left(NoSuchElementErr))) => badUrls          -> responses
          case ((badUrls, responses), (url, _))                    => (url :: badUrls) -> responses
        }

      private def extractM[J]: Option[(J, List[UrlAddress])] => Option[J] =
        (k: Option[(J, List[UrlAddress])]) =>
          (for { (r, _) <- k } yield r) match {
            case Some(u) => u.some
            case _       => none[J]
        }

      private def computeResult[D]: List[(UrlAddress, D)] => Option[(D, List[UrlAddress])] =
        (inputs: List[(UrlAddress, D)]) =>
          Either.catchNonFatal {
            val (dRes, urlsRaw) = inputs.groupBy(_._2).maxBy(_._2.size)
            dRes -> urlsRaw.map(_._1)
          }.toOption

      private def requestManyPar[R](
        f: UrlAddress => F[Either[HttpApiErr, R]]
      ): F[List[(UrlAddress, Either[HttpApiErr, R])]] =
        ref.get.flatMap(_.map(url => f(url).map(url -> _)).parSequence)

      private def tryToRichExpectedElement[U](
        f: UrlAddress => F[Either[HttpApiErr, U]],
        urls: List[UrlAddress]
      ): F[(Option[U], List[UrlAddress])] = {
        def loop(urls: List[UrlAddress], inconsistentUrls: List[UrlAddress]): F[(Option[U], List[UrlAddress])] =
          urls.headOption match {
            case Some(url) =>
              f(url).flatMap {
                case Right(potentialElement) =>
                  Logger[F].info(s"Got expected element from $url").map(_ => potentialElement.some -> inconsistentUrls)
                case Left(NoSuchElementErr) =>
                  Logger[F].info(s"Got the empty element from $url.") >> loop(urls.drop(1), inconsistentUrls)
                case _ =>
                  Logger[F]
                    .info(s"Failed to setup connection with $url.")
                    .flatMap(_ => loop(urls.drop(1), url :: inconsistentUrls))
              }
            case None =>
              Logger[F].info(s"Failed to get required element from the network.").map(_ => none -> inconsistentUrls)
          }

        loop(urls, List.empty)
      }
    }

}
