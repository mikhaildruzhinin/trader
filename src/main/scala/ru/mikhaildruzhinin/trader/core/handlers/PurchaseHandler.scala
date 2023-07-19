package ru.mikhaildruzhinin.trader.core.handlers

import ru.mikhaildruzhinin.trader.client.BaseInvestApiClient
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.mikhaildruzhinin.trader.core.TypeCode._
import ru.mikhaildruzhinin.trader.core.wrappers.ShareWrapper
import ru.mikhaildruzhinin.trader.database.connection.Connection
import ru.mikhaildruzhinin.trader.database.tables.SharesTable
import slick.dbio.DBIO

object PurchaseHandler extends Handler {
  override def apply()(implicit appConfig: AppConfig,
                       investApiClient: BaseInvestApiClient,
                       connection: Connection): Int = {

    val purchasedShares: Seq[ShareWrapper] = wrapPersistedShares(Uptrend)
      .map(
        s => ShareWrapper
          .builder()
          .fromWrapper(s)
          .withPurchasePrice(s.currentPrice)
          .build()
      )

    connection.run(
      DBIO.sequence(purchasedShares.map(s => {
        SharesTable.update(s.figi, s.toShareType(Purchased))
      }))
    )

    log.info(s"Purchased: ${purchasedShares.length.toString}")
    purchasedShares.length
  }
}
