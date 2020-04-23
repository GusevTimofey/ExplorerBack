package encry.explorer.env

import java.util.concurrent.{ Executors, ThreadFactory }

import cats.effect.{ ConcurrentEffect, Resource }
import com.google.common.util.concurrent.ThreadFactoryBuilder
import encry.explorer.core.settings.ChainObserverSettings
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder

import scala.concurrent.{ ExecutionContext, ExecutionContextExecutor }

final case class HttpClientContext[F[_]](client: Client[F])

object HttpClientContext {
  def create[F[_]: ConcurrentEffect](
    chainObserverSettings: ChainObserverSettings
  ): Resource[F, HttpClientContext[F]] = {
    val tf: ThreadFactory = new ThreadFactoryBuilder()
      .setNameFormat("http-api-thread-pool-%d")
      .setDaemon(false)
      .setPriority(Thread.NORM_PRIORITY)
      .build()
    val ec: ExecutionContextExecutor = ExecutionContext.fromExecutor(
      Executors.newFixedThreadPool(chainObserverSettings.observerClientThreadsQuantity, tf)
    )
    BlazeClientBuilder[F](ec).resource.map(client => HttpClientContext(client))
  }
}
