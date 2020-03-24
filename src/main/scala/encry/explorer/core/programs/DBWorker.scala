package encry.explorer.core.programs

import fs2.Stream

trait DBWorker[F[_]] {
  def run: Stream[F, Unit]
}

object DBWorker {
//  def apply[F[_]]() = new DBWorker[F] {
//    override def run: Stream[F, Unit] = ???
//  }
}
