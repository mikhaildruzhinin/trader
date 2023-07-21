package ru.mikhaildruzhinin.trader.core.handlers

import com.typesafe.scalalogging.Logger
import ru.mikhaildruzhinin.trader.client.BaseInvestApiClient
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.mikhaildruzhinin.trader.core.TypeCode
import ru.mikhaildruzhinin.trader.core.wrappers.ShareWrapper
import ru.mikhaildruzhinin.trader.database.connection.Connection
import ru.mikhaildruzhinin.trader.database.tables.SharesTable
import ru.tinkoff.piapi.core.InvestApi

trait Handler {
  val log: Logger = Logger(getClass.getName)

  protected def wrapPersistedShares(typeCode: TypeCode)
                                   (implicit appConfig: AppConfig,
                                    connection: Connection): Seq[ShareWrapper] = {

    val wrappedShares: Seq[ShareWrapper] = connection
      .run(SharesTable.filterByTypeCode(typeCode.code))
      .flatten
      .map(s => ShareWrapper
        .builder()
        .fromModel(s)
        .build()
      )

    log.info(s"Got ${wrappedShares.length} shares of type: $typeCode(${typeCode.code})")
    wrappedShares
  }

  protected def updateCurrentPrices(shares: Seq[ShareWrapper])
                  (implicit appConfig: AppConfig,
                   investApi: InvestApi,
                   investApiClient: BaseInvestApiClient): Seq[ShareWrapper] = investApiClient
      .getLastPrices(shares.map(_.figi))
      .zip(shares)
      .map(x => ShareWrapper
        .builder()
        .fromWrapper(x._2)
        .withCurrentPrice(Some(x._1.getPrice))
        .withUpdateTime(Some(x._1.getTime))
        .build()
      )

  def apply()(implicit appConfig: AppConfig,
              investApi: InvestApi,
              investApiClient: BaseInvestApiClient,
              connection: Connection): Int
}
