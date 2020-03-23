package encry.explorer.chain.observer.services

import cats.effect.Sync
import cats.syntax.functor._
import cats.syntax.option._
import encry.explorer.chain.observer.http.api.models.{ HttpApiBlock, HttpApiNodeInfo }
import encry.explorer.core._
import encry.explorer.core.{ HeaderHeight, Id }
import org.http4s.client.Client
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.{ Method, Request, Uri }
import io.circe.generic.auto._
//todo These imports have to be declared in the scope. Doesn't compile without it. Intellij IDEA bug.
//todo import encry.explorer.chain.observer.http.api.models.boxes.HttpApiBox._
//todo import encry.explorer.chain.observer.http.api.models.directives.HttpApiDirective._
import encry.explorer.chain.observer.http.api.models.boxes.HttpApiBox._
import encry.explorer.chain.observer.http.api.models.directives.HttpApiDirective._

trait NodeObserver[F[_]] {

  def getBestBlockIdAt(height: HeaderHeight): F[Option[String]]

  def getBlockBy(id: Id): F[Option[HttpApiBlock]]

  def getInfo: F[HttpApiNodeInfo]

  def getBestFullHeight: F[Int]

  def getBestHeadersHeight: F[Int]

}

object NodeObserver {

  def apply[F[_]: Sync](
    client: Client[F],
    url: UrlAddress,
  ): NodeObserver[F] = new NodeObserver[F] {

    override def getBestBlockIdAt(height: HeaderHeight): F[Option[String]] =
      client
        .expect[List[String]](
          getRequest(s"$url/history/at/$height")
        )
        .map {
          case Nil       => none[String]
          case head :: _ => head.some
        }

    override def getBlockBy(id: Id): F[Option[HttpApiBlock]] =
      client
        .expectOption[HttpApiBlock](getRequest(s"$url/history/$id"))

    override def getInfo: F[HttpApiNodeInfo] =
      client.expect[HttpApiNodeInfo](getRequest(s"$url/info"))

    override def getBestFullHeight: F[Int] =
      getInfo.map(_.bestFullHeaderId)

    override def getBestHeadersHeight: F[Int] =
      getInfo.map(_.bestHeaderId)

    private def getRequest(url: String): Request[F] =
      Request[F](Method.GET, Uri.unsafeFromString(url))
  }
}
