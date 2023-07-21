package ru.mikhaildruzhinin.trader.core.handlers

import ru.mikhaildruzhinin.trader.client.BaseInvestApiClient
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.mikhaildruzhinin.trader.core.TypeCode._
import ru.mikhaildruzhinin.trader.core.wrappers.ShareWrapper
import ru.mikhaildruzhinin.trader.database.connection.Connection
import ru.mikhaildruzhinin.trader.database.tables.SharesTable
import slick.dbio.DBIO

object MonitorHandler extends Handler {
  override def apply()(implicit appConfig: AppConfig,
                       investApiClient: BaseInvestApiClient,
                       connection: Connection): Int = {

    val purchasedShares: Seq[ShareWrapper] = wrapPersistedShares(Purchased)
    val updatesShares: Seq[ShareWrapper] = updateCurrentPrices(purchasedShares)
    val (sharesToSell: Seq[ShareWrapper], sharesToKeep: Seq[ShareWrapper]) = updatesShares
      .partition(_.roi < Some(BigDecimal(0)))

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
