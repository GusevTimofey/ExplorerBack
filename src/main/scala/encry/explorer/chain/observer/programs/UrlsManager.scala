package encry.explorer.chain.observer.programs

import cats.effect.concurrent.Ref
import cats.effect.{ Concurrent, Sync, Timer }
import cats.instances.try_._
import cats.syntax.functor._
import encry.explorer.core.UrlAddress
import encry.explorer.core.settings.ExplorerSettings
import fs2.Stream
import fs2.concurrent.Queue

import scala.concurrent.duration._
import scala.util.Try

//todo add work with `connectedWith: List[UrlAddress]`.
//todo add insertion network statistic to db
//todo add work with limited number of urls
//todo add last up time computation
trait UrlsManager[F[_]] {

  def run: Stream[F, Unit]

  def getAvailableUrls: F[List[UrlAddress]]
}

object UrlsManager {
  def apply[F[_]: Sync: Timer: Concurrent](
    incomingUnreachableUrls: Queue[F, UrlAddress],
    incomingUrlStatistic: Queue[F, UrlCurrentState],
    SR: ExplorerSettings
  ): F[UrlsManager[F]] =
    for {
      localUrlsInfo <- Ref.of[F, Map[UrlAddress, UrlInfo]](
                        SR.httpClientSettings.encryNodes
                          .map(url => UrlAddress.fromString[Try](url).get -> UrlInfo.empty)
                          .toMap
                      )
    } yield
      new UrlsManager[F] {
        override def run: Stream[F, Unit] =
          processIncomingUnreachableUrls concurrently cleanupMetered concurrently processIncomingUrlStatistic

        override def getAvailableUrls: F[List[UrlAddress]] = localUrlsInfo.get.map(_.keys.toList)

        private def processIncomingUnreachableUrls: Stream[F, Unit] =
          incomingUnreachableUrls.dequeue.evalMap { newUrl =>
            localUrlsInfo.get.map { thisUrls =>
              thisUrls.get(newUrl) match {
                case Some(info) => thisUrls.updated(newUrl, info.copy(failedPingsNumber = info.failedPingsNumber + 1))
                case None       => thisUrls
              }
            }
          }.void

        private def cleanupMetered: Stream[F, Unit] =
          Stream(())
            .covary[F]
            .metered(10.seconds)
            .evalMap(_ => localUrlsInfo.update(_.filter(elem => elem._2.failedPingsNumber < 3)))

        private def processIncomingUrlStatistic: Stream[F, Unit] =
          incomingUrlStatistic.dequeue.evalMap { newStat =>
            localUrlsInfo.update { thisUrls =>
              thisUrls.get(newStat.url) match {
                case Some(info) =>
                  thisUrls.updated(
                    newStat.url,
                    info.copy(bestFullHeight = newStat.bestFullHeight, bestHeaderHeight = newStat.bestHeaderHeight)
                  )
                case None => thisUrls
              }
            }
          }

      }

  final case class UrlInfo(
    bestFullHeight: Int,
    bestHeaderHeight: Int,
    failedPingsNumber: Int,
    lastPingTime: Long,
    connectedWith: List[UrlAddress]
  )
  object UrlInfo {
    def empty: UrlInfo = UrlInfo(-1, -1, -1, -1, List.empty)
  }

  final case class UrlCurrentState(
    url: UrlAddress,
    bestFullHeight: Int,
    bestHeaderHeight: Int,
    connectedWith: List[UrlAddress]
  )
}
