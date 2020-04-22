package encry.explorer.core.services

import cats.effect.{ Concurrent, Timer }
import cats.instances.try_._
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.{ ~>, Monad }
import encry.explorer.chain.observer.http.api.models.HttpApiBlock
import encry.explorer.core.Id
import encry.explorer.core.db.algebra.LiftConnectionIO
import encry.explorer.core.db.models.{ HeaderDBModel, InputDBModel, OutputDBModel, TransactionDBModel }
import encry.explorer.core.db.repositories.{
  HeaderRepository,
  InputRepository,
  OutputRepository,
  TransactionRepository
}
import fs2.Stream
import fs2.concurrent.Queue
import io.chrisdavenport.log4cats.Logger

import scala.util.Try

trait DBService[F[_]] {
  def run: Stream[F, Unit]

  def getBestHeightFromDB: F[Int]
}

object DBService {
  def apply[F[_]: Timer: Concurrent: Logger, CI[_]: Monad: LiftConnectionIO](
    blocksToInsert: Queue[F, HttpApiBlock],
    forkBlocks: Queue[F, String],
    headerRepository: HeaderRepository[CI],
    inputRepository: InputRepository[CI],
    outputRepository: OutputRepository[CI],
    transactionRepository: TransactionRepository[CI],
    transformF: CI ~> F
  ): DBService[F] =
    new DBService[F] {

      override def run: Stream[F, Unit] = updateChain concurrently resolveFork

      override def getBestHeightFromDB: F[Int] =
        transformF(headerRepository.getBestHeight).map(_.getOrElse(0))

      //todo resolve used inputs
      private def httpBlockToDBComponents(inputBlock: HttpApiBlock): F[DbComponentsToInsert] = {
        val dbHeader: HeaderDBModel = HeaderDBModel.fromHttpApi(inputBlock)
        val dbInputs: List[InputDBModel] =
          inputBlock.payload.transactions.flatMap(tx => InputDBModel.fromHttpApi(tx, inputBlock.header.id))
        val dbOutputs: List[OutputDBModel] =
          inputBlock.payload.transactions.flatMap(tx => OutputDBModel.fromHttpApi(tx, inputBlock.header.id))
        val dbTransactions: List[TransactionDBModel] = TransactionDBModel.fromHttpApi(inputBlock)
        DbComponentsToInsert(dbHeader, dbInputs, dbOutputs, dbTransactions).pure[F]
      }

      private def updateChain: Stream[F, Unit] = blocksToInsert.dequeue.evalMap(insertNew)

      private def resolveFork: Stream[F, Unit] =
        forkBlocks.dequeue.evalMap { id =>
          transformF(headerRepository.updateBestChainField(Id.fromString[Try](id).get, statement = false))
        }.void

      private def insertNew(httpBlock: HttpApiBlock): F[Unit] =
        for {
          dbComponents <- httpBlockToDBComponents(httpBlock)
          _ <- transformF(for {
                _ <- headerRepository.insert(dbComponents.dbHeader)
                _ <- transactionRepository.insertMany(dbComponents.dbTransactions)
                _ <- inputRepository.insertMany(dbComponents.dbInputs)
                _ <- outputRepository.insertMany(dbComponents.dbOutputs)
              } yield ())
          _ <- Logger[F].info(
                s"Block inserted with id: ${httpBlock.header.id} at height ${httpBlock.header.height}. " +
                  s"Txs number is: ${httpBlock.payload.transactions.size}."
              )
        } yield ()

    }

  final case class DbComponentsToInsert(
    dbHeader: HeaderDBModel,
    dbInputs: List[InputDBModel],
    dbOutputs: List[OutputDBModel],
    dbTransactions: List[TransactionDBModel]
  )
}
