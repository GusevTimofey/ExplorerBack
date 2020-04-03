package encry.explorer.chain.observer.services

import cats.effect.{ Sync, Timer }
import cats.syntax.applicativeError._
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.functor._
import encry.explorer.chain.observer.errors.{ AddressIsUnreachable, HttpApiErr, NoSuchElementErr }
import encry.explorer.chain.observer.http.api.models.{ HttpApiBlock, HttpApiNodeInfo, HttpApiPeersInfo }
import encry.explorer.core.UrlAddress
import io.chrisdavenport.log4cats.Logger
import io.circe.generic.auto._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.client.Client
import org.http4s.{ Method, Request, Uri }
import retry.RetryPolicies.{ constantDelay, limitRetries }
import retry.syntax.all._
import retry.{ RetryDetails, RetryPolicy }

import scala.concurrent.duration._

trait ClientService[F[_]] {

  def getBlockIdsAt(height: Int)(from: UrlAddress): F[Either[HttpApiErr, List[String]]]

  def getBestBlockIdAt(height: Int)(from: UrlAddress): F[Either[HttpApiErr, String]]

  def getBlockBy(id: String)(from: UrlAddress): F[Either[HttpApiErr, HttpApiBlock]]

  def getClientInfo(from: UrlAddress): F[Either[HttpApiErr, HttpApiNodeInfo]]

  def getBestFullHeight(from: UrlAddress): F[Either[HttpApiErr, Int]]

  def getBestHeadersHeight(from: UrlAddress): F[Either[HttpApiErr, Int]]

  def getConnectedPeers(from: UrlAddress): F[Either[HttpApiErr, List[HttpApiPeersInfo]]]

  def getLastIds(height: Int, quantity: Int)(from: UrlAddress): F[Either[HttpApiErr, List[String]]]
}

object ClientService {
  def apply[F[_]: Sync: Logger: Timer](client: Client[F]): ClientService[F] = new ClientService[F] {

    private val policy: RetryPolicy[F] = limitRetries[F](maxRetries = 2) join constantDelay[F](1.seconds)

    override def getBlockIdsAt(height: Int)(from: UrlAddress): F[Either[HttpApiErr, List[String]]] =
      doRequestOfList[String](request = s"$from/history/at/$height", from = from)

    override def getLastIds(height: Int, quantity: Int)(from: UrlAddress): F[Either[HttpApiErr, List[String]]] =
      doRequestOfList[String](request = s"$from/history?limit=$quantity&offset=${height - quantity + 1}", from = from)

    override def getConnectedPeers(from: UrlAddress): F[Either[HttpApiErr, List[HttpApiPeersInfo]]] =
      doRequestOfList[HttpApiPeersInfo](request = s"$from/peers/connected", from = from)

    override def getBestBlockIdAt(height: Int)(from: UrlAddress): F[Either[HttpApiErr, String]] =
      getBlockIdsAt(height)(from).map {
        case Left(err)        => err.asLeft[String]
        case Right(head :: _) => head.asRight[HttpApiErr]
      }

    override def getBlockBy(id: String)(from: UrlAddress): F[Either[HttpApiErr, HttpApiBlock]] =
      doRequestOfOption[HttpApiBlock](s"$from/history/$id", from)

    private def getInfoFrame(url: UrlAddress, optionalInfo: String = ""): F[Either[HttpApiErr, HttpApiNodeInfo]] =
      doRequestOfOption[HttpApiNodeInfo](s"$url/info", url, optionalInfo)

    override def getClientInfo(from: UrlAddress): F[Either[HttpApiErr, HttpApiNodeInfo]] = getInfoFrame(from)

    override def getBestFullHeight(from: UrlAddress): F[Either[HttpApiErr, Int]] =
      getInfoFrame(from, optionalInfo = s"/extract/fullHeight").map { _.map { _.fullHeight } }

    override def getBestHeadersHeight(from: UrlAddress): F[Either[HttpApiErr, Int]] =
      getInfoFrame(from, optionalInfo = s"extract/headersHeight").map { _.map { _.headersHeight } }

    private def doRequestOfList[R](request: String, from: UrlAddress): F[Either[HttpApiErr, List[R]]] =
      retryRequest(
        client.expect[List[R]](getRequest(request)).map {
          case Nil  => NoSuchElementErr.asLeft[List[R]]
          case list => list.asRight[HttpApiErr]
        },
        request,
        from
      )

    private def doRequestOfOption[R](
      request: String,
      from: UrlAddress,
      optionalRequestInfo: String = ""
    ): F[Either[HttpApiErr, R]] =
      retryRequest(
        client.expectOption[R](getRequest(request)).map(Either.fromOption(_, NoSuchElementErr)),
        request + optionalRequestInfo,
        from
      )

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
            .map(_ => AddressIsUnreachable(urlAddress).asLeft[M])
        }

    private def retryDetailsLogMessage: RetryDetails => String = {
      case RetryDetails.GivingUp(totalRetries, totalDelay) =>
        s"going to give up. Total retries number is: $totalRetries. Total delay is: ${totalDelay.toSeconds}s"
      case RetryDetails.WillDelayAndRetry(nextDelay, _, cumulativeDelay) =>
        s"going to perform next request after: ${nextDelay.toSeconds}s. Total delay is: ${cumulativeDelay.toSeconds}s"
    }
  }
}
