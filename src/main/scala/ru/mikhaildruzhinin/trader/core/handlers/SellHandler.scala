package ru.mikhaildruzhinin.trader.core.handlers

import ru.mikhaildruzhinin.trader.client.BaseInvestApiClient
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.mikhaildruzhinin.trader.core.ShareWrapper
import ru.mikhaildruzhinin.trader.core.TypeCode._
import ru.mikhaildruzhinin.trader.database.connection.{Connection, DatabaseConnection}
import ru.mikhaildruzhinin.trader.database.tables.shares.{SharesLogTable, SharesOperationsTable}

import scala.concurrent.Await

object SellHandler extends Handler {

  override def apply()(implicit appConfig: AppConfig,
                       investApiClient: BaseInvestApiClient,
                       connection: Connection): Int = {

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

    val sellSharesNum: Int = Await
      .result(
        DatabaseConnection.asyncRun(
          Vector(
            SharesOperationsTable.insert(sharesToSell.map(_.getShareTuple(Sold))),
            SharesLogTable.insert(sharesToSell.map(_.getShareTuple(Sold)))
          )
        ),
        appConfig.slick.await.duration
      )
      .headOption
      .flatten
      .getOrElse(-1)

    log.info(s"Sell: ${sellSharesNum.toString}")
    sharesToSell.foreach(s => log.info(s.toString))
    sellSharesNum
  }
}
