package encry.explorer.core

trait RunnableProgram[F[_]] {
  def run: fs2.Stream[F, Unit]
}
