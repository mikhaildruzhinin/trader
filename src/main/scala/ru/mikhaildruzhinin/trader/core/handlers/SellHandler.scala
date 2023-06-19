package ru.mikhaildruzhinin.trader.core.handlers

import com.github.kagkarlsson.scheduler.task._
import com.typesafe.scalalogging.Logger
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.mikhaildruzhinin.trader.core.ShareWrapper
import ru.mikhaildruzhinin.trader.core.TypeCode._
import ru.mikhaildruzhinin.trader.database.connection.{Connection, DatabaseConnection}
import ru.mikhaildruzhinin.trader.database.tables.{SharesLogTable, SharesTable}

import scala.concurrent.Await

class SellHandler[T](implicit appConfig: AppConfig,
                     connection: Connection) extends VoidExecutionHandler[T] {

  val log: Logger = Logger(getClass.getName)

  override def execute(taskInstance: TaskInstance[T],
                       executionContext: ExecutionContext): Unit = {

    val sharesToSell: Seq[ShareWrapper] = ShareWrapper
      .getPersistedShares(Kept)
      .map(
        s => ShareWrapper(
          shareWrapper = s,
          startingPrice = s.startingPrice,
          purchasePrice = s.currentPrice,
          currentPrice = s.currentPrice,
          updateTime = s.updateTime
        )
      )

    val sellSharesNum: Seq[Option[Int]] = Await.result(
      DatabaseConnection.asyncRun(
        Vector(
          SharesTable.insert(sharesToSell.map(_.getShareTuple(Sold))),
          SharesLogTable.insert(sharesToSell.map(_.getShareTuple(Sold)))
        )
      ),
      appConfig.slick.await.duration
    )
    log.info(s"Sell: ${sellSharesNum.headOption.flatten.getOrElse(-1).toString}")
    sharesToSell.foreach(s => log.info(s.toString))
  }
}

object SellHandler {
  def apply()(implicit appConfig: AppConfig,
              connection: Connection) = new SellHandler[Void]()
}
