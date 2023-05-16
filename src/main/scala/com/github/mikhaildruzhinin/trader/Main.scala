package com.github.mikhaildruzhinin.trader

import com.typesafe.scalalogging.Logger
import pureconfig.ConfigSource
import pureconfig.generic.auto.exportReader
import ru.tinkoff.piapi.contract.v1.{CandleInterval, HistoricCandle, InstrumentStatus, Quotation, Share}
import ru.tinkoff.piapi.core.{InstrumentsService, InvestApi, MarketDataService}
import ru.tinkoff.piapi.core.utils.MapperUtils.quotationToBigDecimal

import java.time.{DayOfWeek, Instant, LocalDate, ZoneOffset}
import java.time.temporal.ChronoUnit
import scala.jdk.CollectionConverters._
import scala.math.BigDecimal.{RoundingMode, javaBigDecimal2bigDecimal}

object Main extends App {
  case class ShareWrapper(figi: String,
                          lot: Int,
                          currency: String,
                          name: String,
                          exchange: String,
                          openPrice: Option[Quotation] = None,
                          closePrice: Option[Quotation] = None) {

    def this(share: Share) = this(
      share.getFigi,
      share.getLot,
      share.getCurrency,
      share.getName,
      share.getExchange
    )

    def this(shareWrapper: ShareWrapper,
             openPrice: Option[Quotation],
             closePrice: Option[Quotation]) = this(
      shareWrapper.figi,
      shareWrapper.lot,
      shareWrapper.currency,
      shareWrapper.name,
      shareWrapper.exchange,
      openPrice,
      closePrice
    )

    lazy val uptrendPct: Option[BigDecimal] = {
      (openPrice, closePrice) match {
        case (Some(openPriceValue), Some(closePriceValue)) =>
          val close = quotationToBigDecimal(closePriceValue)
          val open = quotationToBigDecimal(openPriceValue)
          Some(
            ((close - open) / open * 100)
              .setScale(config.pctScale, RoundingMode.HALF_UP)
          )
        case _ => None
      }
    }

    lazy val uptrendAbs: Option[BigDecimal] = {
      (openPrice, uptrendPct) match {
        case (Some(openPriceValue), Some(uptrendPct)) =>
          Some(
            (quotationToBigDecimal(openPriceValue) * lot * uptrendPct / 100)
              .setScale(config.priceScale, RoundingMode.HALF_UP)
          )
        case _ => None
      }
    }

    lazy val closePriceAbs: Option[BigDecimal] = {
      closePrice match {
        case Some(close) => Some(quotationToBigDecimal(close) * lot)
        case _ => None
      }
    }

    override def toString: String = s"$name, ${uptrendPct.getOrElse(-1)}, ${uptrendAbs.getOrElse(-1)}"
  }

  object ShareWrapper {
    def apply(share: Share): ShareWrapper = {
      new ShareWrapper(share)
    }

    def apply(shareWrapper: ShareWrapper,
              openPrice: Option[Quotation],
              closePrice: Option[Quotation]): ShareWrapper = {
      new ShareWrapper(shareWrapper, openPrice, closePrice)
    }
  }

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

  val config: Config = ConfigSource.default.loadOrThrow[Config]
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
