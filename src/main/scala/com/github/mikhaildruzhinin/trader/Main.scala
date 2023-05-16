package com.github.mikhaildruzhinin.trader

import com.typesafe.scalalogging.Logger
import pureconfig.ConfigSource
import pureconfig.generic.auto.exportReader
import ru.tinkoff.piapi.contract.v1.{CandleInterval, HistoricCandle, InstrumentStatus, Quotation, Share}
import ru.tinkoff.piapi.core.{InstrumentsService, InvestApi, MarketDataService}

import java.time.{DayOfWeek, Instant, LocalDate, ZoneOffset}
import java.time.temporal.ChronoUnit
import scala.jdk.CollectionConverters._

object Main extends App {
  def getCandles(shareWrapper: ShareWrapper,
                 from: Instant,
                 to: Instant,
                 interval: CandleInterval): List[HistoricCandle] = {
    marketDataService
      .getCandlesSync(shareWrapper.figi, from, to, interval)
      .asScala
      .toList
  }

  val log: Logger = Logger(getClass.getName.stripSuffix("$"))

  implicit lazy val config: Config = ConfigSource.default.loadOrThrow[Config]
  val token: String = config.tinkoffInvestApiToken
  val api: InvestApi = InvestApi.createSandbox(token)
  val instrumentService: InstrumentsService = api.getInstrumentsService
  val marketDataService: MarketDataService = api.getMarketDataService

  val currentDayOfWeek: DayOfWeek = LocalDate.now.getDayOfWeek
  val startDayInstant: Instant = LocalDate.now.atStartOfDay.toInstant(ZoneOffset.UTC)

  val shares: Iterator[Share] = instrumentService
    .getSharesSync(InstrumentStatus.INSTRUMENT_STATUS_BASE)
    .asScala
    .iterator
    .filter(
      s => s.getExchange == config.exchange
        && s.getApiTradeAvailableFlag
    )

  val filteredShares: Iterator[Share] = currentDayOfWeek match {
    case DayOfWeek.SATURDAY => shares.filter(s => s.getWeekendFlag)
    case DayOfWeek.SUNDAY => shares.filter(s => s.getWeekendFlag)
    case _ => shares
  }

  val wrappedShares: Iterator[ShareWrapper] = filteredShares
    .map(
      s => {
        val shareWrapper = ShareWrapper(s)
        val startDayInstant: Instant = LocalDate
          .now
          .atStartOfDay
          .toInstant(ZoneOffset.UTC)

        val openPrice: Option[Quotation] = getCandles(
          shareWrapper = shareWrapper,
          from = startDayInstant.plus(7, ChronoUnit.HOURS),
          to = startDayInstant.plus(7 + 1, ChronoUnit.HOURS),
          interval = CandleInterval.CANDLE_INTERVAL_5_MIN
        ).headOption match {
          case Some(candle) => Some(candle.getOpen)
          case None => None
        }

        ShareWrapper(shareWrapper, openPrice, None)
      }
    )

  val wrappedSharesUptrend: List[ShareWrapper] = wrappedShares
    .map(
      s => {
        val closePrice: Option[Quotation] = getCandles(
          shareWrapper = s,
          from = startDayInstant.plus(7 + 2, ChronoUnit.HOURS),
          to = startDayInstant.plus(7 + 2 + 1, ChronoUnit.HOURS),
          interval = CandleInterval.CANDLE_INTERVAL_5_MIN
        ).headOption match {
          case Some(candle) => Some(candle.getClose)
          case None => None
        }
        ShareWrapper(s, s.openPrice, closePrice)
      }
    )
    .filter(_.uptrendPct > Some(config.uptrendThresholdPct))
    .toList
    .sortBy(_.uptrendAbs)
    .reverse
    .take(config.numUptrendShares)

  wrappedSharesUptrend.foreach(s => log.info(s.toString))
}
