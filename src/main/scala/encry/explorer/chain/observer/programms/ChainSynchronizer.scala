package encry.explorer.chain.observer.programms

import fs2.Stream

trait ChainSynchronizer[F[_]] {
  def run: Stream[F, Unit]
}

object ChainSynchronizer {

}