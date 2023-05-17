package com.github.mikhaildruzhinin.trader

import com.typesafe.scalalogging.Logger
import ru.tinkoff.piapi.contract.v1.{CandleInterval, HistoricCandle}
import ru.tinkoff.piapi.core.MarketDataService
import ru.tinkoff.piapi.core.exception.ApiRuntimeException

import java.time.Instant
import scala.annotation.tailrec
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.{Failure, Success, Try}

object util {
  val log: Logger = Logger(getClass.getName.stripSuffix("$"))

  @tailrec
  def getCandles(shareWrapper: ShareWrapper,
                 from: Instant,
                 to: Instant,
                 interval: CandleInterval)
                (implicit config: Config,
                 marketDataService: MarketDataService): List[HistoricCandle] = {

    Try {
      marketDataService
        .getCandlesSync(shareWrapper.figi, from, to, interval)
        .asScala
        .toList
    } match {
      case Success(candles) => candles
      case Failure(exception: ApiRuntimeException) =>
        log.error(exception.toString)
        Thread.sleep(config.tinkoffInvestApi.rateLimitPauseMillis)
        getCandles(shareWrapper, from, to, interval)
      case Failure(exception) =>
        log.error(exception.toString)
        throw exception
    }
  }
}
