package ru.mikhaildruzhinin.trader.core.handlers

import ru.mikhaildruzhinin.trader.client.BaseInvestApiClient
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.mikhaildruzhinin.trader.core.ShareWrapper
import ru.mikhaildruzhinin.trader.core.TypeCode._
import ru.mikhaildruzhinin.trader.database.connection.Connection
import ru.mikhaildruzhinin.trader.database.tables.SharesTable

import scala.concurrent.Await

object SellHandler extends Handler {

  override def apply()(implicit appConfig: AppConfig,
                       investApiClient: BaseInvestApiClient,
                       connection: Connection): Int = {

    val sharesToSell: Seq[ShareWrapper] = wrapPersistedShares(Purchased)
      .map(
        s => ShareWrapper(
          shareWrapper = s,
          startingPrice = s.startingPrice,
          purchasePrice = s.currentPrice,
          currentPrice = s.currentPrice,
          updateTime = s.updateTime
        )
      )

    Await.result(
      connection.asyncRun(
        Vector(
          SharesTable.updateTypeCode(figis = sharesToSell.map(s => s.figi), Sold.code),
        )
      ),
      appConfig.slick.await.duration
    )

    log.info(s"Sell: ${sharesToSell.length.toString}")
    sharesToSell.foreach(s => log.info(s.toString))
    sharesToSell.length
  }
}
