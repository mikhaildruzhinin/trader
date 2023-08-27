package ru.mikhaildruzhinin.trader.core.services.base

import ru.mikhaildruzhinin.trader.core.dto.{HistoricCandleDTO, ShareDTO}

import scala.concurrent.Future

trait BaseHistoricCandleService {
  def getWrappedCandles(shares: Seq[ShareDTO]): Future[Seq[HistoricCandleDTO]]
}
