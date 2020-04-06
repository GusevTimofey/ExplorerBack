package encry.explorer.chain.observer.programs

import cats.effect.{ Sync, Timer }
import cats.instances.try_._
import cats.syntax.flatMap._
import cats.syntax.functor._
import encry.explorer.chain.observer.http.api.models.{ HttpApiNodeInfo, HttpApiPeersInfo }
import encry.explorer.chain.observer.programs.UrlsManager.UrlCurrentState
import encry.explorer.chain.observer.services.{ ClientService, GatheringService }
import encry.explorer.core.UrlAddress
import fs2.Stream
import fs2.concurrent.Queue
import io.chrisdavenport.log4cats.Logger

import scala.concurrent.duration._
import scala.util.Try

trait NetworkObserver[F[_]] {
  def run: Stream[F, Unit]
}

object NetworkObserver {
  def apply[F[_]: Sync: Timer: Logger](
    clientService: ClientService[F],
    gatheringService: GatheringService[F],
    urlsManager: UrlsManager[F],
    outgoingUrlStatistic: Queue[F, UrlCurrentState],
  ): NetworkObserver[F] = new NetworkObserver[F] {
    override def run: Stream[F, Unit] =
      Stream(()).repeat
        .covary[F]
        .metered(30.seconds)
        .flatTap(_ => Stream.eval(Logger[F].info(s"Performing getInfo request for all known urls.")))
        .evalMap(_ => getNetworkInfo)

    private def getNetworkInfo: F[Unit] =
      for {
        urls          <- urlsManager.getAvailableUrls
        nodesInfo     <- gatheringService.gatherAll(clientService.getClientInfo, urls)
        connectedInfo <- gatheringService.gatherAll(clientService.getConnectedPeers, urls)
        toUrlsManager = mergeConnectedWithInfo(nodesInfo, connectedInfo)
        _             <- Logger[F].info(s"Urls statistics are: ${toUrlsManager.mkString(",")}.")
        _             <- outgoingUrlStatistic.enqueue(Stream.emits(toUrlsManager)).compile.drain
      } yield ()

    private def mergeConnectedWithInfo(
      info: List[(UrlAddress, HttpApiNodeInfo)],
      connected: List[(UrlAddress, List[HttpApiPeersInfo])]
    ): List[UrlCurrentState] = {
      val infoMap: Map[UrlAddress, HttpApiNodeInfo]             = info.toMap
      val connectedMap: Map[UrlAddress, List[HttpApiPeersInfo]] = connected.toMap
      infoMap.filterKeys(connectedMap.contains).foldLeft(List.empty[UrlCurrentState]) {
        case (acc, (url, info)) =>
          connectedMap.get(url) match {
            case Some(value) =>
              UrlCurrentState(
                url,
                info.fullHeight,
                info.headersHeight,
                value.flatMap(l => UrlAddress.fromInfoAddress[Try](l.address).toOption)
              ) :: acc
            case None => acc
          }
      }
    }
  }
}
