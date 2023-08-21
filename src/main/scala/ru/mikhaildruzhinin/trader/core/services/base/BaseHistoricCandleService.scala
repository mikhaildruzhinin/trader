package ru.mikhaildruzhinin.trader.core.services.base

import ru.mikhaildruzhinin.trader.core.wrappers.{HistoricCandleWrapper, ShareWrapper}

import scala.concurrent.Future

trait BaseHistoricCandleService {
  def getWrappedCandles(shares: Seq[ShareWrapper]): Future[Seq[HistoricCandleWrapper]]
}
