package ru.mikhaildruzhinin.trader.core.services.base

import ru.mikhaildruzhinin.trader.core.dto.{PriceDTO, ShareDTO}

import scala.concurrent.Future

trait BasePriceService {
  def getCurrentPrices(shares: Seq[ShareDTO]): Future[Seq[PriceDTO]]
}
