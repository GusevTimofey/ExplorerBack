package encry.explorer.core.services

import cats.effect.Timer
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.{ Applicative, Monad }
import encry.explorer.chain.observer.http.api.models.HttpApiBlock
import encry.explorer.core.RunnableProgram
import encry.explorer.core.db.models.{ HeaderDBModel, InputDBModel, OutputDBModel, TransactionDBModel }
import encry.explorer.core.db.repositories.{
  HeaderRepository,
  InputRepository,
  OutputRepository,
  TransactionRepository
}
import fs2.Stream
import fs2.concurrent.Queue

import scala.concurrent.duration._

trait DBService[F[_]] extends RunnableProgram[F] {
  def run: Stream[F, Unit]
}

object DBService {
  def apply[F[_]: Applicative: Monad: Timer](
    queue: Queue[F, HttpApiBlock],
    headerRepository: HeaderRepository[F],
    inputRepository: InputRepository[F],
    outputRepository: OutputRepository[F],
    transactionRepository: TransactionRepository[F]
  ): DBService[F] = new DBService[F] {

    override def run: Stream[F, Unit] =
      Stream(()).repeat
        .covary[F]
        .metered(1.seconds)
        .evalMap(_ => insert)

    final case class DbComponentsToInsert(
      dbHeader: HeaderDBModel,
      dbInputs: List[InputDBModel],
      dbOutputs: List[OutputDBModel],
      dbTransactions: List[TransactionDBModel]
    )

    def hhtpBlockToDBComponents(inputBlock: HttpApiBlock): F[DbComponentsToInsert] = {
      val dbHeader: HeaderDBModel                  = HeaderDBModel.fromHttpApi(inputBlock)
      val dbInputs: List[InputDBModel]             = inputBlock.payload.transactions.flatMap(InputDBModel.fromHttpApi)
      val dbOutputs: List[OutputDBModel]           = inputBlock.payload.transactions.flatMap(OutputDBModel.fromHttpApi)
      val dbTransactions: List[TransactionDBModel] = TransactionDBModel.fromHttpApi(inputBlock)
      DbComponentsToInsert(dbHeader, dbInputs, dbOutputs, dbTransactions).pure[F]
    }

    def insert: F[Unit] =
      for {
        httpBlock    <- queue.dequeue1
        dbComponents <- hhtpBlockToDBComponents(httpBlock)
        _            <- headerRepository.insert(dbComponents.dbHeader)
        _            <- inputRepository.insertMany(dbComponents.dbInputs)
        _            <- outputRepository.insertMany(dbComponents.dbOutputs)
        _            <- transactionRepository.insertMany(dbComponents.dbTransactions)
      } yield ()

  }
}
