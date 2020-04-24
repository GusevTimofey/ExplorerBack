package encry.explorer.chain.observer.services

import cats.effect.{ Sync, Timer }
import cats.syntax.applicativeError._
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.option._
import encry.explorer.chain.observer.errors.{ AddressIsUnreachable, HttpApiErr, NoSuchElementErr }
import encry.explorer.chain.observer.http.api.models.{ HttpApiBlock, HttpApiNodeInfo, HttpApiPeersInfo }
import encry.explorer.core.UrlAddress
import encry.explorer.env.HasExplorerContext
import io.chrisdavenport.log4cats.Logger
import io.circe._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.client.Client
import org.http4s.{ InvalidMessageBodyFailure, Method, Request, Uri }
import retry.RetryPolicies.{ constantDelay, limitRetries }
import retry.syntax.all._
import retry.{ RetryDetails, RetryPolicy }

import scala.concurrent.duration._

trait ClientService[F[_]] {

  def getBlockIdsAt(height: Int)(from: UrlAddress): F[HttpApiErr Either List[String]]

  def getBestBlockIdAt(height: Int)(from: UrlAddress): F[HttpApiErr Either String]

  def getBlockBy(id: String)(from: UrlAddress): F[HttpApiErr Either HttpApiBlock]

  def getClientInfo(from: UrlAddress): F[HttpApiErr Either HttpApiNodeInfo]

  def getBestFullHeight(from: UrlAddress): F[HttpApiErr Either Int]

  def getBestHeadersHeight(from: UrlAddress): F[HttpApiErr Either Int]

  def getConnectedPeers(from: UrlAddress): F[HttpApiErr Either List[HttpApiPeersInfo]]

  def getLastIds(height: Int, quantity: Int)(from: UrlAddress): F[HttpApiErr Either List[String]]
}

object ClientService {
  def apply[F[_]: Sync: Timer](client: Client[F])(implicit ec: HasExplorerContext[F]): ClientService[F] =
    new ClientService[F] {

      private val policy: RetryPolicy[F] =
        limitRetries[F](maxRetries = 2) join constantDelay[F](1.seconds)

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
          case _                => NoSuchElementErr.asLeft[String]
        }

      override def getBlockBy(id: String)(from: UrlAddress): F[Either[HttpApiErr, HttpApiBlock]] =
        doRequestOfOption[HttpApiBlock](s"$from/history/$id")

      private def getInfoFrame(url: UrlAddress, optionalInfo: String = ""): F[Either[HttpApiErr, HttpApiNodeInfo]] =
        doRequestOfOption[HttpApiNodeInfo](s"$url/info", optionalInfo)

      override def getClientInfo(from: UrlAddress): F[Either[HttpApiErr, HttpApiNodeInfo]] = getInfoFrame(from)

      override def getBestFullHeight(from: UrlAddress): F[Either[HttpApiErr, Int]] =
        getInfoFrame(from, optionalInfo = s"/extract/fullHeight").map(_.map(_.fullHeight))

      override def getBestHeadersHeight(from: UrlAddress): F[Either[HttpApiErr, Int]] =
        getInfoFrame(from, optionalInfo = s"extract/headersHeight").map(_.map(_.headersHeight))

      private def doRequestOfList[R: Decoder](request: String, from: UrlAddress): F[Either[HttpApiErr, List[R]]] =
        retryRequest(
          client
            .expect[List[R]](getRequest(request))
            .handleErrorWith {
              case InvalidMessageBodyFailure(_, cause) =>
                ec.ask(_.logger)
                  .flatMap(_.info(s"Request $request failed with cause $cause.").map(_ => List.empty[R]))
            }
            .map {
              case Nil  => NoSuchElementErr.asLeft[List[R]]
              case list => list.asRight[HttpApiErr]
            },
          request
        )

      private def doRequestOfOption[R: Decoder](
        request: String,
        optionalRequestInfo: String = ""
      ): F[Either[HttpApiErr, R]] =
        retryRequest(
          client
            .expectOption[R](getRequest(request))
            .handleErrorWith {
              case InvalidMessageBodyFailure(_, cause) =>
                ec.ask(_.logger)
                  .flatMap(_.info(s"Request $request failed with cause $cause.").map(_ => none[R]))
            }
            .map(Either.fromOption(_, NoSuchElementErr)),
          request + optionalRequestInfo
        )

      private def getRequest(url: String): Request[F] = Request[F](Method.GET, Uri.unsafeFromString(url))

      private def retryRequest[M](
        request: F[Either[HttpApiErr, M]],
        requestContent: String
      ): F[Either[HttpApiErr, M]] =
        request
          .retryingOnAllErrors(
            policy,
            (_, details: RetryDetails) =>
              ec.ask(_.logger)
                .flatMap(
                  _.info(
                    s"Failed to perform request: $requestContent. " +
                      s"Retry details are: ${retryDetailsLogMessage(details)}. " +
                      s"Going to retry this request again."
                  )
                )
          )
          .flatTap(result =>
            ec.ask(_.logger)
              .flatMap(_.debug(s"Request $requestContent finished successfully. Result is: $result."))
          )
          .handleErrorWith { _: Throwable =>
            ec.ask(_.logger)
              .flatMap(
                _.info(s"Retrying attempts for request $requestContent have ended.")
                  .map(_ => AddressIsUnreachable.asLeft[M])
              )
          }

      private def retryDetailsLogMessage: RetryDetails => String = {
        case RetryDetails.GivingUp(totalRetries, totalDelay) =>
          s"going to give up. Total retries number is: $totalRetries. Total delay is: ${totalDelay.toSeconds}s"
        case RetryDetails.WillDelayAndRetry(nextDelay, _, cumulativeDelay) =>
          s"going to perform next request after: ${nextDelay.toSeconds}s. Total delay is: ${cumulativeDelay.toSeconds}s"
      }
    }
}
