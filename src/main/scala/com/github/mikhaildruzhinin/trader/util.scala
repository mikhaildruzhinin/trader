package com.github.mikhaildruzhinin.trader

import ru.tinkoff.piapi.contract.v1.{CandleInterval, HistoricCandle}
import ru.tinkoff.piapi.core.MarketDataService

import java.time.Instant
import scala.jdk.CollectionConverters.CollectionHasAsScala

object util {
  def getCandles(shareWrapper: ShareWrapper,
                 from: Instant,
                 to: Instant,
                 interval: CandleInterval)
                (implicit marketDataService: MarketDataService): List[HistoricCandle] = {
    marketDataService
      .getCandlesSync(shareWrapper.figi, from, to, interval)
      .asScala
      .toList
  }
}
