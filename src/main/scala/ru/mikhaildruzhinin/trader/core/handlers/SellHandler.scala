package ru.mikhaildruzhinin.trader.core.handlers

import ru.mikhaildruzhinin.trader.client.base.BaseInvestApiClient
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.mikhaildruzhinin.trader.core.TypeCode._
import ru.mikhaildruzhinin.trader.core.wrappers.ShareWrapper
import ru.mikhaildruzhinin.trader.database.connection.Connection
import ru.mikhaildruzhinin.trader.database.tables.SharesTable
import ru.tinkoff.piapi.core.InvestApi
import slick.dbio.DBIO

object SellHandler extends Handler {

  override def apply()(implicit appConfig: AppConfig,
                       investApiClient: BaseInvestApiClient,
                       connection: Connection): Int = {

    val sharesToSell: Seq[ShareWrapper] = wrapPersistedShares(Purchased)
      .map(
        s => ShareWrapper
          .builder()
          .fromWrapper(s)
          .build()
      )

    val sharesToSellNum: Int = connection.run(
      DBIO.sequence(sharesToSell.map(s => {
        SharesTable.update(s.figi, s.toShareType(Sold))
      }))
    ).flatten.length

    log.info(s"Sell: ${sharesToSellNum.toString}")
    sharesToSell.foreach(s => log.info(s.toString))
    sharesToSellNum
  }
}
