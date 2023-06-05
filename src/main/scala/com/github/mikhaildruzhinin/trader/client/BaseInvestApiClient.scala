package com.github.mikhaildruzhinin.trader.client

import com.github.mikhaildruzhinin.trader.config.AppConfig
import ru.tinkoff.piapi.contract.v1._

import java.time.Instant

trait BaseInvestApiClient {
  def getCandles(figi: String, from: Instant, to: Instant, interval: CandleInterval)
                (implicit appConfig: AppConfig): List[HistoricCandle]

  def getShares(implicit appConfig: AppConfig): List[Share]

  def getLastPrices(figi: Seq[String])(implicit appConfig: AppConfig): Seq[LastPrice]
}
