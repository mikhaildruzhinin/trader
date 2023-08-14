package ru.mikhaildruzhinin.trader.client.base

import ru.tinkoff.piapi.contract.v1.{CandleInterval, HistoricCandle, LastPrice, Share}

import java.time.Instant
import scala.concurrent.Future

abstract class BaseInvestApiClient {

  def getCandles(figi: String,
                 from: Instant,
                 to: Instant,
                 interval: CandleInterval): Future[Seq[HistoricCandle]]

  def getShares: Future[Seq[Share]]

  def getLastPrices(figi: Seq[String]): Future[Seq[LastPrice]]
}
