package ru.mikhaildruzhinin.trader.core.services.base

import ru.mikhaildruzhinin.trader.core.wrappers.{HistoricCandleWrapper, ShareWrapper}
import ru.tinkoff.piapi.contract.v1.HistoricCandle

import scala.concurrent.Future

trait BaseHistoricCandleService {
  def wrapCandles(candles: Seq[Option[HistoricCandle]]): Future[Seq[HistoricCandleWrapper]]

  def getWrappedCandles(shares: Seq[ShareWrapper]): Future[Seq[HistoricCandleWrapper]]
}
