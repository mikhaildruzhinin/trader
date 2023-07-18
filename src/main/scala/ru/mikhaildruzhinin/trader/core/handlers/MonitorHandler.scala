package ru.mikhaildruzhinin.trader.core.handlers

import ru.mikhaildruzhinin.trader.client.BaseInvestApiClient
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.mikhaildruzhinin.trader.core.ShareWrapper
import ru.mikhaildruzhinin.trader.core.TypeCode._
import ru.mikhaildruzhinin.trader.database.connection.Connection
import ru.mikhaildruzhinin.trader.database.tables.SharesTable
import slick.dbio.DBIO

object MonitorHandler extends Handler {

  private def splitShares(shares: Seq[ShareWrapper])
                              (implicit appConfig: AppConfig,
                               investApiClient: BaseInvestApiClient,
                               connection: Connection): (Seq[ShareWrapper], Seq[ShareWrapper]) = {

    investApiClient
      .getLastPrices(shares.map(_.figi))
      .zip(shares)
      .map(x => ShareWrapper(shareWrapper = x._2, lastPrice = x._1))
      .partition(_.roi <= Some(BigDecimal(0)))
  }

  override def apply()(implicit appConfig: AppConfig,
                       investApiClient: BaseInvestApiClient,
                       connection: Connection): Int = {

    val purchasedShares: Seq[ShareWrapper] = wrapPersistedShares(Purchased)
    val (sharesToSell: Seq[ShareWrapper], sharesToKeep: Seq[ShareWrapper]) = splitShares(purchasedShares)

    val sharesToSellNum: Int = connection.run(
      DBIO.sequence(sharesToSell.map(s => {
        SharesTable.update(s.figi, s.toShareType(Sold))
      }))
    ).flatten.length

    connection.run(
      DBIO.sequence(sharesToKeep.map(s => {
        SharesTable.update(s.figi, s.toShareType(Purchased))
      }))
    )

    log.info(s"Sell: ${sharesToSellNum.toString}")
    sharesToSellNum
  }
}
