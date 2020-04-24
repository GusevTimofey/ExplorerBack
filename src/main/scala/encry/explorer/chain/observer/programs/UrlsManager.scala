package encry.explorer.chain.observer.programs

import java.text.SimpleDateFormat
import java.util.Date

import cats.effect.concurrent.Ref
import cats.effect.{ Concurrent, Timer }
import cats.instances.list._
import cats.instances.try_._
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.traverse._
import encry.explorer.chain.observer.BanTime
import encry.explorer.chain.observer.http.api.models.HttpApiNodeInfo
import encry.explorer.chain.observer.services.{ ClientService, GatheringService }
import encry.explorer.core.UrlAddress
import encry.explorer.core.settings.ExplorerSettingsContext
import encry.explorer.env.{ ContextClientQueues, ContextSharedQueues }
import encry.explorer.events.processing.{ NewNode, UnavailableNode }
import fs2.Stream
import io.chrisdavenport.log4cats.Logger

import scala.concurrent.duration._
import scala.util.Try

//todo add insertion network statistic to db
trait UrlsManager[F[_]] {

  def run: Stream[F, Unit]

  def getAvailableUrls: F[List[UrlAddress]]
}

object UrlsManager {
  def apply[F[_]: Timer: Concurrent: Logger](
    clientService: ClientService[F],
    gatheringService: GatheringService[F],
    SR: ExplorerSettingsContext
  )(implicit sharedQC: ContextSharedQueues[F], clientQC: ContextClientQueues[F]): F[UrlsManager[F]] =
    for {
      localUrlsInfo <- Ref.of[F, Map[UrlAddress, UrlInfo]](
                        SR.httpClientSettings.encryNodes
                          .map(url => UrlAddress.fromString[Try](url).get -> UrlInfo.empty)
                          .toMap
                      )
      incomingUnreachableUrls <- clientQC.ask(_.unreachableUrlsQueue)
      incomingUrlStatistic    <- clientQC.ask(_.urlStatisticQueue)
      eventsQueue             <- sharedQC.ask(_.eventsQueue)
      bannedUrls              <- Ref.of[F, Map[UrlAddress, BanTime]](Map.empty)
    } yield new UrlsManager[F] {
      override def run: Stream[F, Unit] =
        processIncomingUnreachableUrls concurrently cleanupMetered concurrently processIncomingUrlStatistic

      override def getAvailableUrls: F[List[UrlAddress]] = localUrlsInfo.get.map(_.keys.toList)

      private def processIncomingUnreachableUrls: Stream[F, Unit] =
        incomingUnreachableUrls.dequeue.evalMap { newUrl =>
          Logger[F].info(s"Urls manager got new unreachable url $newUrl. Going to update its statistic.") >>
            localUrlsInfo.update { thisUrls =>
              thisUrls.get(newUrl) match {
                case Some(info) => thisUrls.updated(newUrl, info.copy(failedPingsNumber = info.failedPingsNumber + 1))
                case None       => thisUrls
              }
            } >> eventsQueue.enqueue1(UnavailableNode(newUrl.value.value))
        }.void

      private def cleanupMetered: Stream[F, Unit] =
        Stream(()).repeat
          .covary[F]
          .metered(10.seconds)
          .flatTap { _ =>
            Stream.eval {
              Logger[F].info(s"Performing cleanup operation.") >>
                bannedUrls.get.flatMap(urls => Logger[F].info(s"${formLogsForBanned(urls)}")) >>
                localUrlsInfo.get.flatMap(urls => Logger[F].info(s"local urls are: ${urls.mkString(",")}."))
            }
          }
          .evalMap { _ =>
            localUrlsInfo.get.flatMap { urls =>
              urls.collect {
                case (address, info) if info.failedPingsNumber > SR.httpClientSettings.maxPingTimes =>
                  bannedUrls.update(_.updated(address, BanTime(System.currentTimeMillis())))
              }.toList.sequence
            } >>
              localUrlsInfo.update(_.filter(elem => elem._2.failedPingsNumber <= SR.httpClientSettings.maxPingTimes))
          }
          .evalMap { _ =>
            bannedUrls.update(_.filter(elem => elem._2.value > SR.httpClientSettings.maxTimeBan)) >>
              bannedUrls.get.flatMap(urls => Logger[F].info(s"${formLogsForBanned(urls)}"))
          }

      private def processIncomingUrlStatistic: Stream[F, Unit] =
        incomingUrlStatistic.dequeue.evalMap { newStat =>
          Logger[F].info(s"Urls manager got new statistic: $newStat from incoming urls statistic.") >>
            localUrlsInfo.modify { thisUrls =>
              thisUrls.get(newStat.url) match {
                case Some(info) =>
                  thisUrls.updated(
                    newStat.url,
                    info.copy(bestFullHeight = newStat.bestFullHeight, bestHeaderHeight = newStat.bestHeaderHeight)
                  ) -> newStat.connectedWith
                case None => thisUrls -> List.empty
              }
            }.flatMap(trySetupConnections)
        }

      private def trySetupConnections(list: List[UrlAddress]): F[Unit] =
        for {
          banned     <- bannedUrls.get
          connected  <- localUrlsInfo.get
          forConnect = list.filterNot(url => banned.contains(url) && connected.contains(url))
          allowed = forConnect.take(
            if (forConnect.size + connected.size > SR.httpClientSettings.maxConnections)
              SR.httpClientSettings.maxConnections - connected.size
            else forConnect.size
          )
          response <- if (allowed.nonEmpty) gatheringService.gatherAll(clientService.getClientInfo, allowed)
                     else List.empty[(UrlAddress, HttpApiNodeInfo)].pure[F]
          _ <- eventsQueue.enqueue(Stream.emits(response.map(l => NewNode(l._1.value.value)))).compile.drain
          _ <- localUrlsInfo.update { urls =>
                response.foldLeft(urls) {
                  case (urlsLocal, (url, info)) =>
                    urlsLocal.updated(url, UrlInfo.fromHttpInfo(info, url))
                }
              }
        } yield ()

      private def timeFormatter(time: Long): String =
        new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date(time))

      private def formLogsForBanned(urls: Map[UrlAddress, BanTime]): String =
        s"Banned peers are: ${urls.map {
          case (address, time) => address -> timeFormatter(time.value)
        }.mkString(",")}."

    }

  final case class UrlInfo(
    bestFullHeight: Int,
    bestHeaderHeight: Int,
    failedPingsNumber: Int,
    connectedWith: List[UrlAddress]
  )
  object UrlInfo {
    def empty: UrlInfo = UrlInfo(0, 0, 0, List.empty)
    def fromHttpInfo(info: HttpApiNodeInfo, urlAddress: UrlAddress): UrlInfo =
      UrlInfo(
        bestFullHeight = info.fullHeight,
        bestHeaderHeight = info.headersHeight,
        0,
        List.empty
      )
  }

  final case class UrlCurrentState(
    url: UrlAddress,
    bestFullHeight: Int,
    bestHeaderHeight: Int,
    connectedWith: List[UrlAddress]
  )
}
