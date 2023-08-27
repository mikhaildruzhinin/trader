package ru.mikhaildruzhinin.trader.core.services.impl

import ru.mikhaildruzhinin.trader.client.base.BaseInvestApiClient
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.mikhaildruzhinin.trader.core.services.base.BasePriceService
import ru.mikhaildruzhinin.trader.core.dto.{PriceDTO, ShareDTO}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PriceService(investApiClient: BaseInvestApiClient)
                  (implicit appConfig: AppConfig) extends BasePriceService {

  override def getCurrentPrices(shares: Seq[ShareDTO]): Future[Seq[PriceDTO]] = for {
    lastPrices <- investApiClient.getLastPrices(shares.map(_.figi))
    wrappedLastPrices <- Future { lastPrices.map(lp => PriceDTO(lp)) }
  } yield wrappedLastPrices
}
