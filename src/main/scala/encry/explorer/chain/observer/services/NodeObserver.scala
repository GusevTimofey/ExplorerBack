package encry.explorer.chain.observer.services

import cats.effect.{ Sync, Timer }
import cats.syntax.applicativeError._
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.functor._
import encry.explorer.chain.observer.errors._
import encry.explorer.chain.observer.http.api.models.{ HttpApiBlock, HttpApiNodeInfo, HttpApiPeersInfo }
import encry.explorer.core.{ HeaderHeight, Id, UrlAddress }
import io.chrisdavenport.log4cats.Logger
import io.circe.generic.auto._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.client.Client
import org.http4s.{ Method, Request, Uri }
import retry.RetryPolicies._
import retry._
import retry.syntax.all._

import scala.concurrent.duration._

trait NodeObserver[F[_]] {

  def getBestBlockIdAt(height: HeaderHeight)(from: UrlAddress): F[Either[HttpApiErr, String]]

  def getBlockBy(id: Id)(from: UrlAddress): F[Either[HttpApiErr, HttpApiBlock]]

  def getInfo(from: UrlAddress): F[Either[HttpApiErr, HttpApiNodeInfo]]

  def getBestFullHeight(from: UrlAddress): F[Either[HttpApiErr, Int]]

  def getBestHeadersHeight(from: UrlAddress): F[Either[HttpApiErr, Int]]

  def getConnectedPeers(from: UrlAddress): F[Either[HttpApiErr, List[HttpApiPeersInfo]]]

  def getLastIds(height: Int, quantity: Int)(from: UrlAddress): F[Either[HttpApiErr, List[String]]]

}

object NodeObserver {

  def apply[F[_]: Sync: Logger: Timer](client: Client[F]): NodeObserver[F] = new NodeObserver[F] {

    private val policy: RetryPolicy[F] = limitRetries[F](maxRetries = 2) join constantDelay[F](0.25.seconds)

    override def getBestBlockIdAt(height: HeaderHeight)(from: UrlAddress): F[Either[HttpApiErr, String]] = {
      val request: String = s"$from/history/at/$height"
      retryRequest(
        client
          .expect[List[String]](getRequest(request))
          .map(elems => Either.fromOption(elems.headOption, NoSuchElementErr)),
        request,
        from
      )
    }

    override def getBlockBy(id: Id)(from: UrlAddress): F[Either[HttpApiErr, HttpApiBlock]] = {
      val request: String = s"$from/history/$id"
      retryRequest(
        client.expectOption[HttpApiBlock](getRequest(request)).map(Either.fromOption(_, NoSuchElementErr)),
        request,
        from
      )
    }

    override def getInfo(from: UrlAddress): F[Either[HttpApiErr, HttpApiNodeInfo]] =
      getInfoFrame(from, requestName = "")

    override def getBestFullHeight(from: UrlAddress): F[Either[HttpApiErr, Int]] =
      getInfoFrame(from, requestName = s"._extract<>fullHeight").map { _.map { _.fullHeight } }

    override def getBestHeadersHeight(from: UrlAddress): F[Either[HttpApiErr, Int]] =
      getInfoFrame(from, requestName = s"._extract<>headersHeight").map { _.map { _.headersHeight } }

    override def getConnectedPeers(from: UrlAddress): F[Either[HttpApiErr, List[HttpApiPeersInfo]]] = {
      val request: String = s"$from/peers/connected"
      retryRequest(
        client.expect[List[HttpApiPeersInfo]](getRequest(request)).map {
          case Nil            => NoSuchElementErr.asLeft[List[HttpApiPeersInfo]]
          case peers @ _ :: _ => peers.asRight[HttpApiErr]
        },
        request,
        from
      )
    }

    override def getLastIds(height: Int, quantity: Int)(from: UrlAddress): F[Either[HttpApiErr, List[String]]] = {
      val request: String = s"$from/history?limit=$quantity&offset=${height - quantity + 1}"
      retryRequest(
        client.expect[List[String]](getRequest(request)).map {
          case Nil          => NoSuchElementErr.asLeft[List[String]]
          case ids @ _ :: _ => ids.asRight[HttpApiErr]
        },
        request,
        from
      )
    }

    private def getInfoFrame(url: UrlAddress, requestName: String): F[Either[HttpApiErr, HttpApiNodeInfo]] = {
      val request: String = s"$url/info"
      retryRequest(
        client.expectOption[HttpApiNodeInfo](getRequest(request)).map(Either.fromOption(_, NoSuchElementErr)),
        request + requestName,
        url
      )
    }

    private def getRequest(url: String): Request[F] = Request[F](Method.GET, Uri.unsafeFromString(url))

    private def retryRequest[M](
      request: F[Either[HttpApiErr, M]],
      requestContent: String,
      urlAddress: UrlAddress
    ): F[Either[HttpApiErr, M]] =
      request
        .retryingOnAllErrors(
          policy,
          (err, details: RetryDetails) =>
            Logger[F].info(
              s"Failed to perform request: $requestContent. " +
                s"Retry details are: ${retryDetailsLogMessage(details)}. " +
                s"Request performing failed cause: ${err.getMessage}. " +
                s"Going to retry this request again."
          )
        )
        .flatTap(_ => Logger[F].info(s"Request $requestContent finished successfully."))
        .handleErrorWith { err: Throwable =>
          Logger[F]
            .info(s"Retrying attempts for request $requestContent have ended. Last error is: ${err.getMessage}.")
            .map(_ => AddressIsUnreachable.asLeft[M])
        }

    private def retryDetailsLogMessage: RetryDetails => String = {
      case RetryDetails.GivingUp(totalRetries, totalDelay) =>
        s"going to give up. Total retries number is: $totalRetries. Total delay is: ${totalDelay.toSeconds}s"
      case RetryDetails.WillDelayAndRetry(nextDelay, _, cumulativeDelay) =>
        s"going to perform next request after: ${nextDelay.toSeconds}s. Total delay is: ${cumulativeDelay.toSeconds}s"
    }
  }
}
