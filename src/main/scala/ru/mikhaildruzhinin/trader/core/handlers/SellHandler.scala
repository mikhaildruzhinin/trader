package ru.mikhaildruzhinin.trader.core.handlers

import com.typesafe.scalalogging.Logger
import ru.mikhaildruzhinin.trader.client.BaseInvestApiClient
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.mikhaildruzhinin.trader.core.ShareWrapper
import ru.mikhaildruzhinin.trader.core.TypeCode._
import ru.mikhaildruzhinin.trader.database.connection.{Connection, DatabaseConnection}
import ru.mikhaildruzhinin.trader.database.tables.{SharesLogTable, SharesTable}

import scala.concurrent.Await

object SellHandler extends Handler {

  val log: Logger = Logger(getClass.getName)

  override def apply()(implicit appConfig: AppConfig,
                       investApiClient: BaseInvestApiClient,
                       connection: Connection): Unit = {

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
