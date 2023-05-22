package com.github.mikhaildruzhinin.trader

import com.github.mikhaildruzhinin.trader.config.Config
import com.typesafe.scalalogging.Logger
import ru.tinkoff.piapi.contract.v1._
import ru.tinkoff.piapi.core.exception.ApiRuntimeException

import java.time.Instant
import scala.annotation.tailrec
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

object InvestApiClient {
  val log: Logger = Logger(getClass.getName.stripSuffix("$"))

  @tailrec
  def getCandles(figi: String,
                 from: Instant,
                 to: Instant,
                 interval: CandleInterval)
                (implicit config: Config): List[HistoricCandle] = {

    Try {
      config.tinkoffInvestApi.marketDataService
        .getCandlesSync(figi, from, to, interval)
        .asScala
        .toList
    } match {
      case Success(candles) => candles
      case Failure(exception: ApiRuntimeException) =>
        log.error(exception.toString)
        Thread.sleep(config.tinkoffInvestApi.rateLimitPauseMillis)
        getCandles(figi, from, to, interval)
      case Failure(exception) =>
        log.error(exception.toString)
        throw exception
    }
  }

  @tailrec
  def getShares(implicit config: Config): Iterator[Share] = {

    Try {
      config.tinkoffInvestApi.instrumentService
        .getSharesSync(InstrumentStatus.INSTRUMENT_STATUS_BASE)
        .asScala
        .iterator
        .filter(
          s => s.getExchange == config.exchange.name
            && s.getApiTradeAvailableFlag
        )
    } match {
      case Success(shares) => shares
      case Failure(exception: ApiRuntimeException) =>
        log.error(exception.toString)
        Thread.sleep(config.tinkoffInvestApi.rateLimitPauseMillis)
        getShares
      case Failure(exception) =>
        log.error(exception.toString)
        throw exception
    }
  }

  @tailrec
  def getLastPrices(figi: List[String])
                   (implicit config: Config): Iterator[LastPrice] = {

    Try {
      config.tinkoffInvestApi.marketDataService
        .getLastPricesSync(figi.asJava)
        .asScala
        .iterator
    } match {
      case Success(lastPrices) => lastPrices
      case Failure(exception: ApiRuntimeException) =>
        log.error(exception.toString)
        Thread.sleep(config.tinkoffInvestApi.rateLimitPauseMillis)
        getLastPrices(figi)
      case Failure(exception) =>
        log.error(exception.toString)
        throw exception
    }
  }
}
