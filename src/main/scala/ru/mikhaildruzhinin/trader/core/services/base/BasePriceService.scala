package ru.mikhaildruzhinin.trader.core.services.base

import ru.mikhaildruzhinin.trader.core.wrappers.{PriceWrapper, ShareWrapper}

import scala.concurrent.Future

trait BasePriceService {
  def getCurrentPrices(shares: Seq[ShareWrapper]): Future[Seq[PriceWrapper]]
}
