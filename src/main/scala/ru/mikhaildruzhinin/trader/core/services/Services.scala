package ru.mikhaildruzhinin.trader.core.services

import ru.mikhaildruzhinin.trader.client.base.BaseInvestApiClient
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.mikhaildruzhinin.trader.core.services.base._
import ru.mikhaildruzhinin.trader.core.services.impl._
import ru.mikhaildruzhinin.trader.database.connection.Connection
import ru.mikhaildruzhinin.trader.database.tables.ShareDAO

case class Services private(shareService: BaseShareService,
                            historicCandleService: BaseHistoricCandleService,
                            priceService: BasePriceService,
                            accountService: BaseAccountService)

object Services {
  def apply(investApiClient: BaseInvestApiClient,
            connection: Connection,
            shareDAO: ShareDAO)
           (implicit appConfig: AppConfig): Services = new Services(
    new ShareService(investApiClient, connection, shareDAO),
    new HistoricCandleService(investApiClient),
    new PriceService(investApiClient),
    new AccountService(investApiClient)
  )
}
