package encry.explorer.chain.observer.services

import cats.Functor
import cats.effect.{ Sync, Timer }
import cats.syntax.functor._
import cats.syntax.option._
import cats.syntax.flatMap._
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import cats.instances.option._
import encry.explorer.chain.observer.http.api.models.{ HttpApiBlock, HttpApiNodeInfo, HttpApiPeersInfo }
import encry.explorer.core._
import encry.explorer.core.{ HeaderHeight, Id }
import org.http4s.client.Client
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.{ Method, Request, Uri }
import io.circe.generic.auto._
import io.chrisdavenport.log4cats.Logger
import retry._
import retry.syntax.all._
//todo These imports have to be declared in the scope. Doesn't compile without it. Intellij IDEA bug.
//todo import encry.explorer.chain.observer.http.api.models.boxes.HttpApiBox._
//todo import encry.explorer.chain.observer.http.api.models.directives.HttpApiDirective._
import encry.explorer.chain.observer.http.api.models.boxes.HttpApiBox._
import encry.explorer.chain.observer.http.api.models.directives.HttpApiDirective._

trait NodeObserver[F[_]] {

  def getBestBlockIdAt(height: HeaderHeight, from: UrlAddress): F[Option[String]]

  def getBlockBy(id: Id, from: UrlAddress): F[Option[HttpApiBlock]]

  def getInfo(from: UrlAddress): F[Option[HttpApiNodeInfo]]

  def getBestFullHeight(from: UrlAddress): F[Option[Int]]

  def getBestHeadersHeight(from: UrlAddress): F[Option[Int]]

  def getConnectedPeers(from: UrlAddress): F[List[HttpApiPeersInfo]]

}

object NodeObserver {

  def apply[F[_]: Sync: Logger: Timer](
    client: Client[F]
  ): NodeObserver[F] = new NodeObserver[F] {

    private val policy: RetryPolicy[F] = RetryPolicies.limitRetries[F](3)

    override def getBestBlockIdAt(height: HeaderHeight, url: UrlAddress): F[Option[String]] =
      retryRequest[Option[String]](
        client
          .expect[List[String]](
            getRequest(s"$url/history/at/$height")
          )
          .map {
            case Nil       => none[String]
            case head :: _ => head.some
          },
        s"Get best block id at height $height",
        none[String]
      )

    override def getBlockBy(id: Id, url: UrlAddress): F[Option[HttpApiBlock]] =
      retryRequest[Option[HttpApiBlock]](
        client
          .expectOption[HttpApiBlock](getRequest(s"$url/history/$id")),
        s"Get block with id: $id",
        none[HttpApiBlock]
      )

    override def getInfo(url: UrlAddress): F[Option[HttpApiNodeInfo]] =
      retryRequest[Option[HttpApiNodeInfo]](
        client.expectOption[HttpApiNodeInfo](getRequest(s"$url/info")),
        "Get node info",
        none[HttpApiNodeInfo]
      )

    override def getBestFullHeight(url: UrlAddress): F[Option[Int]] =
      Functor[F].compose[Option].map(getInfo(url))(_.bestFullHeaderId)

    override def getBestHeadersHeight(url: UrlAddress): F[Option[Int]] =
      Functor[F].compose[Option].map(getInfo(url))(_.bestHeaderId)

    override def getConnectedPeers(url: UrlAddress): F[List[HttpApiPeersInfo]] =
      retryRequest[List[HttpApiPeersInfo]](
        client.expect[List[HttpApiPeersInfo]](getRequest(s"$url/peers/connected")),
        "Get http api peer info",
        List.empty[HttpApiPeersInfo]
      )

    private def getRequest(url: String): Request[F] =
      Request[F](Method.GET, Uri.unsafeFromString(url))

    //todo: replace default value with Monoid.empty
    private def retryRequest[Y]: (F[Y], String, Y) => F[Y] =
      (request: F[Y], requestName: String, defaultValue: Y) =>
        request
          .retryingOnAllErrors(
            policy,
            (err, details: RetryDetails) =>
              Logger[F].info(
                s"Failed to perform request: $requestName. " +
                  s"Retry details are: ${retryDetailsLogMessage(details)}. " +
                  s"${err.getMessage}."
            )
          )
          .handleErrorWith { err =>
            Logger[F]
              .info(s"Request $request has failed. Err is: ${err.getMessage} Empty value is going to be returned.") >>
              defaultValue.pure[F]
        }

    private def retryDetailsLogMessage: RetryDetails => String = {
      case RetryDetails.GivingUp(totalRetries, totalDelay) =>
        s"Going to give up. Total retries number is: $totalRetries. Total delay is: ${totalDelay.toSeconds}s"
      case RetryDetails.WillDelayAndRetry(nextDelay, _, cumulativeDelay) =>
        s"Going to perform next request after: ${nextDelay.toSeconds}s. Total delay is: ${cumulativeDelay.toSeconds}s"
    }
  }
}
