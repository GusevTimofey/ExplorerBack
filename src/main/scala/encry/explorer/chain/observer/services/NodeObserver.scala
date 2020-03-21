package encry.explorer.chain.observer.services

import cats.effect.Sync
import cats.syntax.functor._
import cats.syntax.option._
import encry.explorer.core._
import encry.explorer.core.{ HeaderHeight, Id }
import org.http4s.client.Client
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.{ Method, Request, Uri }

trait NodeObserver[F[_]] {

  def getBestBlockIdAt(height: HeaderHeight): F[Option[String]]

  def getInfo: F[Unit]

  def getBestHeight: F[Unit]

}

object NodeObserver {

  def apply[F[_]: Sync](
    client: Client[F],
    url: UrlAddress,
  ) = new NodeObserver[F] {

    override def getBestBlockIdAt(height: HeaderHeight): F[Option[String]] =
      client
        .expect[List[String]](
          getRequest(s"$url/history/at/$height")
        )
        .map {
          case Nil       => none[String]
          case head :: _ => head.some
        }

    override def getInfo: F[Unit] = ???

    override def getBestHeight: F[Unit] = ???

    private def getRequest(url: String): Request[F] = Request[F](Method.GET, Uri.unsafeFromString(url))
  }
}
