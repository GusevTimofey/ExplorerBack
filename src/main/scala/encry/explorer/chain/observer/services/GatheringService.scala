package encry.explorer.chain.observer.services

import cats.Parallel
import cats.effect.Sync
import cats.instances.list._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.option._
import cats.syntax.parallel._
import cats.syntax.traverse._
import encry.explorer.chain.observer.errors._
import encry.explorer.core.UrlAddress
import fs2.Stream
import fs2.concurrent.Queue
import io.chrisdavenport.log4cats.Logger

trait GatheringService[F[_]] {

  def gatherAll[R](
    request: UrlAddress => F[Either[HttpApiErr, R]],
    urls: List[UrlAddress]
  ): F[List[(UrlAddress, R)]]

  def gatherFirst[R](
    request: UrlAddress => F[Either[HttpApiErr, R]],
    urls: List[UrlAddress]
  ): F[Option[R]]

  def gatherOne[R](request: UrlAddress => F[Either[HttpApiErr, R]], url: UrlAddress): F[Option[R]]

  def gatherMany[R](requests: List[UrlAddress => F[Either[HttpApiErr, R]]], urls: List[UrlAddress]): F[List[R]]

  def gatherOneFromMany[R](requests: List[UrlAddress => F[Either[HttpApiErr, R]]], urls: List[UrlAddress]): F[List[R]]

}

object GatheringService {

  def apply[F[_]: Sync: Parallel: Logger](
    clientService: ClientService[F],
    unreachableUrlsQueue: Queue[F, UrlAddress]
  ): GatheringService[F] = new GatheringService[F] {

    override def gatherAll[R](
      request: UrlAddress => F[Either[HttpApiErr, R]],
      urls: List[UrlAddress]
    ): F[List[(UrlAddress, R)]] =
      urls
        .map(url => request(url).map(url -> _))
        .parSequence
        .map(filterResponses)
        .flatMap {
          case (addresses, result) =>
            Logger[F].debug(
              s"Result is gathered from urls: ${urls.mkString(",")} with regime 'gather all' successfully. " +
                s"Unreachable urls are: ${addresses.mkString(",")}. " +
                s"Going to send them to the urls manager. " +
                s"Result is: $result."
            ) >> unreachableUrlsQueue.enqueue(Stream.emits[F, UrlAddress](addresses)).compile.drain.map(_ => result)
        }

    override def gatherFirst[R](
      request: UrlAddress => F[Either[HttpApiErr, R]],
      urls: List[UrlAddress]
    ): F[Option[R]] =
      tryToRichExpectedElement[R](request, urls).flatMap {
        case (maybeR, addresses) =>
          Logger[F].debug(
            s"Result is gathered from urls: ${urls.mkString(",")} with regime 'gather first' successfully. " +
              s"Unreachable urls are: ${addresses.mkString(",")}. " +
              s"Going to send them to the urls manager. " +
              s"Result is: $maybeR."
          ) >> unreachableUrlsQueue.enqueue(Stream.emits[F, UrlAddress](addresses)).compile.drain.map(_ => maybeR)
      }

    override def gatherOne[R](request: UrlAddress => F[Either[HttpApiErr, R]], url: UrlAddress): F[Option[R]] =
      gatherFirst[R](request, List(url))

    override def gatherMany[R](
      requests: List[UrlAddress => F[Either[HttpApiErr, R]]],
      urls: List[UrlAddress]
    ): F[List[R]] =
      requests
        .map(gatherFirst(_, urls))
        .parSequence
        .map(_.flatten)
        .flatTap { res =>
          Logger[F].debug(
            s"Result is gathered from urls: ${urls.mkString(",")} in regime 'gather many' successfully. " +
              s"Result is: ${res.mkString(",")}."
          )
        }

    override def gatherOneFromMany[R](
      requests: List[UrlAddress => F[Either[HttpApiErr, R]]],
      urls: List[UrlAddress]
    ): F[List[R]] = requests.traverse(gatherFirst(_, urls)).map(_.flatten)

    private def filterResponses[R](
      elems: List[(UrlAddress, Either[HttpApiErr, R])]
    ): (List[UrlAddress], List[(UrlAddress, R)]) =
      elems.foldLeft(List.empty[UrlAddress], List.empty[(UrlAddress, R)]) {
        case ((unreachableUrls, r), (url, Right(next)))                => unreachableUrls -> ((url -> next) :: r)
        case (sl, (_, Left(NoSuchElementErr)))                         => sl
        case ((unreachableUrls, r), (url, Left(AddressIsUnreachable))) => (url :: unreachableUrls) -> r
      }

    private def tryToRichExpectedElement[R](
      f: UrlAddress => F[Either[HttpApiErr, R]],
      urls: List[UrlAddress]
    ): F[(Option[R], List[UrlAddress])] = {
      def loop(urls: List[UrlAddress], inconsistentUrls: List[UrlAddress]): F[(Option[R], List[UrlAddress])] =
        urls.headOption match {
          case Some(url) =>
            f(url).flatMap {
              case Right(potentialElement) =>
                Logger[F].debug(s"Got expected element from $url").map(_ => potentialElement.some -> inconsistentUrls)
              case Left(NoSuchElementErr) =>
                Logger[F].debug(s"Got the empty element from $url.") >> loop(urls.drop(1), inconsistentUrls)
              case _ =>
                Logger[F]
                  .debug(s"Failed to setup connection with $url.")
                  .flatMap(_ => loop(urls.drop(1), url :: inconsistentUrls))
            }
          case None =>
            Logger[F].info(s"Failed to get required element from the network.").map(_ => none -> inconsistentUrls)
        }

      loop(urls, List.empty)
    }

  }
}
