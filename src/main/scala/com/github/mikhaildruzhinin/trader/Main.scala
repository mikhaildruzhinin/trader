package com.github.mikhaildruzhinin.trader

import com.google.protobuf.Timestamp
import com.typesafe.scalalogging.Logger
import pureconfig.ConfigSource
import pureconfig.generic.auto.exportReader
import ru.tinkoff.piapi.contract.v1.{CandleInterval, Quotation}
import ru.tinkoff.piapi.core.{InstrumentsService, InvestApi, MarketDataService}

import java.time.temporal.ChronoUnit
import java.time.{DayOfWeek, Instant, LocalDate, ZoneOffset}

object Main extends App {
  val log: Logger = Logger(getClass.getName.stripSuffix("$"))

  implicit lazy val config: Config = ConfigSource.default.loadOrThrow[Config]
  val token: String = config.tinkoffInvestApi.token
  val api: InvestApi = InvestApi.createSandbox(token)
  implicit val instrumentService: InstrumentsService = api.getInstrumentsService
  implicit val marketDataService: MarketDataService = api.getMarketDataService

  val currentDayOfWeek: DayOfWeek = LocalDate.now.getDayOfWeek
  val startDayInstant: Instant = LocalDate.now.atStartOfDay.toInstant(ZoneOffset.UTC)

  val wrappedShares: Iterator[ShareWrapper] = InvestApiClient
    .getShares
    .map(
      s => {
        val shareWrapper = ShareWrapper(s)
        val startDayInstant: Instant = LocalDate
          .now
          .atStartOfDay
          .toInstant(ZoneOffset.UTC)

        val (openPrice: Option[Quotation], updateTime: Option[Timestamp]) = InvestApiClient
          .getCandles(
            shareWrapper = shareWrapper,
            from = startDayInstant.plus(
              config.exchange.startTimeHours,
              ChronoUnit.HOURS
            ),
            to = startDayInstant.plus(
              config.exchange.startTimeHours
                + config.exchange.candleTimedeltaHours,
              ChronoUnit.HOURS
            ),
            interval = CandleInterval.CANDLE_INTERVAL_5_MIN
          ).headOption match {
            case Some(candle) => (Some(candle.getClose), Some(candle.getTime))
            case None => (None, None)
          }

        ShareWrapper(shareWrapper, openPrice, None, updateTime)
      }
    )

  val wrappedSharesUptrend: List[ShareWrapper] = wrappedShares
    .map(
      s => {
        val (closePrice: Option[Quotation], updateTime: Option[Timestamp]) = InvestApiClient
          .getCandles(
            shareWrapper = s,
            from = startDayInstant.plus(
              config.exchange.startTimeHours
                + config.exchange.uptrendCheckTimedeltaHours,
              ChronoUnit.HOURS
            ),
            to = startDayInstant.plus(
              config.exchange.startTimeHours
                + config.exchange.uptrendCheckTimedeltaHours
                + config.exchange.candleTimedeltaHours,
              ChronoUnit.HOURS
            ),
            interval = CandleInterval.CANDLE_INTERVAL_5_MIN
          ).headOption match {
            case Some(candle) => (Some(candle.getClose), Some(candle.getTime))
            case None => (None, None)
          }

        ShareWrapper(s, s.openPrice, closePrice, updateTime)
      }
    )
    .filter(_.uptrendPct > Some(config.uptrendThresholdPct))
    .toList
    .sortBy(_.uptrendAbs)
    .reverse
    .take(config.numUptrendShares)

  wrappedSharesUptrend.foreach(s => log.info(s.toString))
}
