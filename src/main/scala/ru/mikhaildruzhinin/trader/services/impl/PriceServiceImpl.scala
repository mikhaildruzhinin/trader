package ru.mikhaildruzhinin.trader.services.impl

import ru.mikhaildruzhinin.trader.client.InvestApiClient
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.mikhaildruzhinin.trader.models.{PriceModel, ShareModel}
import ru.mikhaildruzhinin.trader.services.PriceService

import scala.concurrent.Future

class PriceServiceImpl(investApiClient: InvestApiClient)
                      (implicit appConfig: AppConfig) extends PriceService {

  override def getCurrentPrices(shares: Seq[ShareModel]): Future[Seq[PriceModel]] = investApiClient
    .getLastPrices(shares.map(_.figi))
}
