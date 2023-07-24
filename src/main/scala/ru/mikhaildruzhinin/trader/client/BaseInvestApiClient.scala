package ru.mikhaildruzhinin.trader.client

import ru.tinkoff.piapi.contract.v1._

import java.time.Instant

abstract class BaseInvestApiClient {

  def getCandles(figi: String,
                 from: Instant,
                 to: Instant,
                 interval: CandleInterval): List[HistoricCandle]

  def getShares: List[Share]

  def getLastPrices(figi: Seq[String]): Seq[LastPrice]
}
