package ru.mikhaildruzhinin.trader.core.handlers

import ru.mikhaildruzhinin.trader.client.BaseInvestApiClient
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.mikhaildruzhinin.trader.core.ShareWrapper
import ru.mikhaildruzhinin.trader.core.TypeCode._
import ru.mikhaildruzhinin.trader.database.connection.Connection
import ru.mikhaildruzhinin.trader.database.tables.SharesTable

import scala.concurrent.Await

object MonitorHandler extends Handler {

  override def apply()(implicit appConfig: AppConfig,
                       investApiClient: BaseInvestApiClient,
                       connection: Connection): Int = {

    val purchasedShares: Seq[ShareWrapper] = wrapPersistedShares(Purchased)

    val (sharesToSell: Seq[ShareWrapper], sharesToKeep: Seq[ShareWrapper]) = investApiClient
      .getLastPrices(purchasedShares.map(_.figi))
      .zip(purchasedShares)
      .map(x => ShareWrapper(x._2, x._1))
      .partition(_.roi <= Some(BigDecimal(0)))

    sharesToSell.foreach(s => log.info(s.toString))

    Await.result(
      connection.asyncRun(
        Vector(
          SharesTable.updateTypeCode(figis = sharesToSell.map(s => s.figi), Sold.code),
        )
      ),
      appConfig.slick.await.duration
    )
      .headOption
      .getOrElse(-1)
    log.info(s"Sell: ${sharesToSell.length.toString}")

    Await.result(
      connection.asyncRun(
        Vector(
          SharesTable.updateTypeCode(figis = sharesToKeep.map(s => s.figi), Kept.code),
        )
      ),
      appConfig.slick.await.duration
    )

    log.info(s"Keep: ${sharesToKeep.length.toString}")

    sharesToKeep.length
  }
}
