package com.github.mikhaildruzhinin.trader

import com.typesafe.scalalogging.Logger
import ru.tinkoff.piapi.contract.v1.{CandleInterval, HistoricCandle, InstrumentStatus, Share}
import ru.tinkoff.piapi.core.exception.ApiRuntimeException

import java.time.{DayOfWeek, Instant, LocalDate}
import scala.annotation.tailrec
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

object InvestApiClient {
  val log: Logger = Logger(getClass.getName.stripSuffix("$"))

  @tailrec
  def getCandles(shareWrapper: ShareWrapper,
                 from: Instant,
                 to: Instant,
                 interval: CandleInterval)
                (implicit config: Config): List[HistoricCandle] = {

    Try {
      config.tinkoffInvestApi.marketDataService
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

  def getShares(implicit config: Config): Iterator[Share] = {
    val currentDayOfWeek: DayOfWeek = LocalDate.now.getDayOfWeek

    val shares: Iterator[Share] = config.tinkoffInvestApi.instrumentService
      .getSharesSync(InstrumentStatus.INSTRUMENT_STATUS_BASE)
      .asScala
      .iterator
      .filter(
        s => s.getExchange == config.exchange.name
          && s.getApiTradeAvailableFlag
      )

    currentDayOfWeek match {
      case DayOfWeek.SATURDAY => shares.filter(s => s.getWeekendFlag)
      case DayOfWeek.SUNDAY => shares.filter(s => s.getWeekendFlag)
      case _ => shares
    }
  }
}
