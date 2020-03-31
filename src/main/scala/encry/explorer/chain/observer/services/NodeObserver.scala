package encry.explorer.chain.observer.services

import cats.Functor
import cats.effect.{ Sync, Timer }
import cats.instances.list._
import cats.instances.option._
import cats.instances.string._
import cats.kernel.Monoid
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.option._
import encry.explorer.chain.observer.http.api.models.HttpApiBlock.instances._
import encry.explorer.chain.observer.http.api.models.HttpApiNodeInfo.instances._
import encry.explorer.chain.observer.http.api.models.{ HttpApiBlock, HttpApiNodeInfo, HttpApiPeersInfo }
import encry.explorer.core.{ HeaderHeight, Id, _ }
import io.chrisdavenport.log4cats.Logger
import io.circe.generic.auto._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.client.Client
import org.http4s.{ Method, Request, Uri }
import retry._
import retry.syntax.all._

trait NodeObserver[F[_]] {

  def getBestBlockIdAt(height: HeaderHeight)(from: UrlAddress): F[Option[String]]

  def getBlockBy(id: Id)(from: UrlAddress): F[Option[HttpApiBlock]]

  def getInfo(from: UrlAddress): F[Option[HttpApiNodeInfo]]

  def getBestFullHeight(from: UrlAddress): F[Option[Int]]

  def getBestHeadersHeight(from: UrlAddress): F[Option[Int]]

  def getConnectedPeers(from: UrlAddress): F[List[HttpApiPeersInfo]]

  def getLastIds(height: Int, quantity: Int)(from: UrlAddress): F[List[String]]

}

object NodeObserver {

  def apply[F[_]: Sync: Logger: Timer](client: Client[F]): NodeObserver[F] = new NodeObserver[F] {

    private val policy: RetryPolicy[F] = RetryPolicies.limitRetries[F](maxRetries = 3)

    override def getLastIds(height: Int, quantity: Int)(from: UrlAddress): F[List[String]] = {
      val request: String = s"$from/history?limit=$quantity&offset=${height - quantity + 1}"
      retryRequest(client.expect[List[String]](getRequest(request)), requestContent = request)
    }

    override def getBestBlockIdAt(height: HeaderHeight)(url: UrlAddress): F[Option[String]] = {
      val request: String = s"$url/history/at/$height"
      retryRequest(client.expect[List[String]](getRequest(request)).map {
        case Nil       => none[String]
        case head :: _ => head.some
      }, requestContent = request)
    }

    override def getBlockBy(id: Id)(url: UrlAddress): F[Option[HttpApiBlock]] = {
      val request: String = s"$url/history/$id"
      retryRequest(client.expectOption[HttpApiBlock](getRequest(request)), requestContent = request)
    }

    private def getInfoFrame(url: UrlAddress, requestName: String): F[Option[HttpApiNodeInfo]] = {
      val request: String = s"$url/info"
      retryRequest(client.expectOption[HttpApiNodeInfo](getRequest(request)), request + requestName)
    }

    override def getInfo(url: UrlAddress): F[Option[HttpApiNodeInfo]] = getInfoFrame(url, requestName = "")

    override def getBestFullHeight(url: UrlAddress): F[Option[Int]] =
      Functor[F]
        .compose[Option]
        .map(getInfoFrame(url, requestName = s".extract:BestFullHeight"))(_.bestFullHeaderId)

    override def getBestHeadersHeight(url: UrlAddress): F[Option[Int]] =
      Functor[F]
        .compose[Option]
        .map(getInfoFrame(url, requestName = s".extract:BestHeadersHeight"))(_.bestHeaderId)

    override def getConnectedPeers(url: UrlAddress): F[List[HttpApiPeersInfo]] = {
      val request: String = s"$url/peers/connected"
      retryRequest(client.expect[List[HttpApiPeersInfo]](getRequest(request)), requestContent = request)
    }

    private def getRequest(url: String): Request[F] = Request[F](Method.GET, Uri.unsafeFromString(url))

    private def retryRequest[M: Monoid](request: F[M], requestContent: String): F[M] =
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
        .handleErrorWith { err: Throwable =>
          Logger[F].info(
            s"Retrying attempts for request $requestContent were finished. " +
              s"Last error is: ${err.getMessage}. " +
              s"Going to return empty default value."
          ) >> Monoid[M].empty.pure[F]
        }

    private def retryDetailsLogMessage: RetryDetails => String = {
      case RetryDetails.GivingUp(totalRetries, totalDelay) =>
        s"Going to give up. Total retries number is: $totalRetries. Total delay is: ${totalDelay.toSeconds}s"
      case RetryDetails.WillDelayAndRetry(nextDelay, _, cumulativeDelay) =>
        s"Going to perform next request after: ${nextDelay.toSeconds}s. Total delay is: ${cumulativeDelay.toSeconds}s"
    }
  }
}
