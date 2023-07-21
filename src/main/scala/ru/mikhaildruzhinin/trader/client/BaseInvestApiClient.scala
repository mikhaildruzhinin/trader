package ru.mikhaildruzhinin.trader.client

import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.tinkoff.piapi.contract.v1._
import ru.tinkoff.piapi.core.InvestApi

import java.time.Instant

abstract class BaseInvestApiClient(implicit appConfig: AppConfig,
                                   investApi: InvestApi) {

  def getCandles(figi: String,
                 from: Instant,
                 to: Instant,
                 interval: CandleInterval): List[HistoricCandle]

  def getShares: List[Share]

  def getLastPrices(figi: Seq[String]): Seq[LastPrice]
}
