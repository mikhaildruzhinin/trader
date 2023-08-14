package ru.mikhaildruzhinin.trader.core.handlers

import ru.mikhaildruzhinin.trader.client.base.BaseInvestApiClient
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.mikhaildruzhinin.trader.core.TypeCode._
import ru.mikhaildruzhinin.trader.core.wrappers.ShareWrapper
import ru.mikhaildruzhinin.trader.database.connection.Connection
import ru.mikhaildruzhinin.trader.database.tables.SharesTable
import ru.tinkoff.piapi.contract.v1._
import ru.tinkoff.piapi.core.InvestApi
import slick.dbio.DBIO

import java.util.UUID

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

//    val account: Account = investApi.getUserService.getAccountsSync.get(0)

    purchasedShares.foreach(
      s => {
//        val r: PostOrderResponse = appConfig
//          .tinkoffInvestApi
//          .api
//          .getOrdersService
//          .postOrderSync(
//            s.figi,
//            1L,
//            Quotation.newBuilder.build(),
//            OrderDirection.ORDER_DIRECTION_BUY,
//            account.getId,
//            OrderType.ORDER_TYPE_BESTPRICE,
//            UUID.randomUUID.toString
//          )
//
//        println(r.getExecutionReportStatus)
      }
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
