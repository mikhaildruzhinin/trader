package ru.mikhaildruzhinin.trader.client

import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.tinkoff.piapi.contract.v1._
import ru.tinkoff.piapi.core.InvestApi

import java.time.Instant

trait BaseInvestApiClient {
  def getCandles(figi: String, from: Instant, to: Instant, interval: CandleInterval)
                (implicit appConfig: AppConfig,
                 investApi: InvestApi): List[HistoricCandle]

  def getShares(implicit appConfig: AppConfig,
                investApi: InvestApi): List[Share]

  def getLastPrices(figi: Seq[String])
                   (implicit appConfig: AppConfig,
                    investApi: InvestApi): Seq[LastPrice]
}
