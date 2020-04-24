package encry.explorer.core.programs

import cats.Monad
import cats.effect.{ Concurrent, Timer }
import cats.instances.try_._
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import encry.explorer.chain.observer.http.api.models.HttpApiBlock
import encry.explorer.core.Id
import encry.explorer.core.db.algebra.LiftConnectionIO
import encry.explorer.core.db.models.{ HeaderDBModel, InputDBModel, OutputDBModel, TransactionDBModel }
import encry.explorer.env.{ HasCoreContext, HasExplorerContext }
import fs2.Stream

import scala.util.Try

trait DBProgram[F[_]] {
  def run: Stream[F, Unit]

  def getBestHeightFromDB: F[Int]
}

object DBProgram {
  def apply[F[_]: Timer: Concurrent, CI[_]: Monad: LiftConnectionIO](
    implicit coreContext: HasCoreContext[F, CI],
    explorerContext: HasExplorerContext[F]
  ): DBProgram[F] =
    new DBProgram[F] {

      override def run: Stream[F, Unit] = updateChain concurrently resolveFork

      override def getBestHeightFromDB: F[Int] =
        coreContext.askF(cxt => cxt.transactor(cxt.repositoriesContext.hr.getBestHeight).map(_.getOrElse(0)))

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

      private def updateChain(): Stream[F, Unit] =
        Stream
          .eval(explorerContext.ask(cxt => cxt.sharedQueuesContext.bestChainBlocks))
          .flatMap(_.dequeue.evalMap(insertNew))

      private def resolveFork: Stream[F, Unit] =
        Stream
          .eval(for {
            forkBlocks       <- explorerContext.ask(_.sharedQueuesContext.forkBlocks)
            transformF       <- coreContext.ask(_.transactor)
            headerRepository <- coreContext.ask(_.repositoriesContext.hr)
          } yield (forkBlocks, transformF, headerRepository))
          .flatMap {
            case (forkBlocks, transformF, headerRepository) =>
              forkBlocks.dequeue.evalMap { id =>
                transformF(headerRepository.updateBestChainField(Id.fromString[Try](id).get, statement = false))
              }.void
          }

      private def insertNew(httpBlock: HttpApiBlock): F[Unit] =
        for {
          dbComponents          <- httpBlockToDBComponents(httpBlock)
          transformF            <- coreContext.ask(_.transactor)
          headerRepository      <- coreContext.ask(_.repositoriesContext.hr)
          transactionRepository <- coreContext.ask(_.repositoriesContext.tr)
          inputRepository       <- coreContext.ask(_.repositoriesContext.ir)
          outputRepository      <- coreContext.ask(_.repositoriesContext.or)
          _ <- transformF(for {
                _ <- headerRepository.insert(dbComponents.dbHeader)
                _ <- transactionRepository.insertMany(dbComponents.dbTransactions)
                _ <- inputRepository.insertMany(dbComponents.dbInputs)
                _ <- outputRepository.insertMany(dbComponents.dbOutputs)
              } yield ())
          logger <- explorerContext.ask(_.logger)
          _ <- logger.info(
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
