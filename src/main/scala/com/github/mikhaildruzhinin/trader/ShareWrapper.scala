package com.github.mikhaildruzhinin.trader

import com.google.protobuf.Timestamp
import ru.tinkoff.piapi.contract.v1.{CandleInterval, Quotation, Share}
import ru.tinkoff.piapi.core.{InstrumentsService, MarketDataService}
import ru.tinkoff.piapi.core.utils.DateUtils.timestampToString
import ru.tinkoff.piapi.core.utils.MapperUtils.quotationToBigDecimal

import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalDate, ZoneOffset}
import scala.math.BigDecimal.{RoundingMode, javaBigDecimal2bigDecimal}

case class ShareWrapper(figi: String,
                        lot: Int,
                        currency: String,
                        name: String,
                        exchange: String,
                        startingPrice: Option[Quotation] = None,
                        currentPrice: Option[Quotation] = None,
                        updateTime: Option[Timestamp] = None)
                       (implicit config: Config) {

  def this(share: Share)(implicit config: Config) = this(
    share.getFigi,
    share.getLot,
    share.getCurrency,
    share.getName,
    share.getExchange
  )

  def this(shareWrapper: ShareWrapper,
           startingPrice: Option[Quotation],
           currentPrice: Option[Quotation],
           updateTime: Option[Timestamp])
          (implicit config: Config) = this(
    shareWrapper.figi,
    shareWrapper.lot,
    shareWrapper.currency,
    shareWrapper.name,
    shareWrapper.exchange,
    startingPrice,
    currentPrice,
    updateTime
  )

  lazy val uptrendPct: Option[BigDecimal] = {
    (startingPrice, currentPrice) match {
      case (Some(startingPriceValue), Some(currentPriceValue)) =>
        Some(
          (
            (quotationToBigDecimal(currentPriceValue) - quotationToBigDecimal(startingPriceValue))
              / quotationToBigDecimal(startingPriceValue) * 100
          ).setScale(config.pctScale, RoundingMode.HALF_UP)
        )
      case _ => None
    }
  }

  lazy val uptrendAbsNoTax: Option[BigDecimal] = {
    (startingPrice, uptrendPct) match {
      case (Some(startingPriceValue), Some(uptrendPctValue)) =>
        Some(
          (quotationToBigDecimal(startingPriceValue) * lot * uptrendPctValue / 100)
            .setScale(config.priceScale, RoundingMode.HALF_UP)
        )
      case _ => None
    }
  }

  lazy val uptrendAbs: Option[BigDecimal] = {
    uptrendAbsNoTax match {
      case Some(uptrendAbsNoTaxValue) =>
        Some(
          (uptrendAbsNoTaxValue * (100 - config.incomeTaxPct) / 100)
            .setScale(config.priceScale, RoundingMode.HALF_UP)
        )
      case _ => None
    }
  }

  lazy val closePriceAbs: Option[BigDecimal] = {
    currentPrice match {
      case Some(close) => Some(quotationToBigDecimal(close) * lot)
      case _ => None
    }
  }

  override def toString: String = {
    new StringBuilder(s"$name, ")
      .append(s"${uptrendPct.getOrElse(-1)}%, ")
      .append(s"${uptrendAbs.getOrElse(-1)} руб., ")
      .append(s"${timestampToString(updateTime.getOrElse(Timestamp.newBuilder.build))}")
      .toString
  }
}

object ShareWrapper {
  private val startDayInstant: Instant = LocalDate
    .now
    .atStartOfDay
    .toInstant(ZoneOffset.UTC)

  def apply(share: Share)
           (implicit config: Config): ShareWrapper = {
    new ShareWrapper(share)
  }

  def apply(shareWrapper: ShareWrapper,
            startingPrice: Option[Quotation],
            currentPrice: Option[Quotation],
            updateTime: Option[Timestamp])
           (implicit config: Config): ShareWrapper = {
    new ShareWrapper(shareWrapper, startingPrice, currentPrice, updateTime)
  }

  def getShares(implicit config: Config,
                instrumentService: InstrumentsService,
                marketDataService: MarketDataService,
                investApiClient: InvestApiClient.type): Iterator[ShareWrapper] = {

    investApiClient
      .getShares
      .map(
        s => {
          val shareWrapper = ShareWrapper(s)

          val (startingPrice: Option[Quotation], updateTime: Option[Timestamp]) = investApiClient
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
            case Some(candle) => (Some(candle.getOpen), Some(candle.getTime))
            case None => (None, None)
          }

          ShareWrapper(shareWrapper, startingPrice, None, updateTime)
        }
      )
  }

  def getUptrendShares(wrappedShares: Iterator[ShareWrapper],
                       checkTimedeltaHours: Int)
                      (implicit config: Config,
                       marketDataService: MarketDataService,
                       investApiClient: InvestApiClient.type): Iterator[ShareWrapper] = {

    wrappedShares
      .map(
        s => {
          val (currentPrice: Option[Quotation], updateTime: Option[Timestamp]) = investApiClient
            .getCandles(
              shareWrapper = s,
              from = startDayInstant.plus(
                config.exchange.startTimeHours
                  + checkTimedeltaHours,
                ChronoUnit.HOURS
              ),
              to = startDayInstant.plus(
                config.exchange.startTimeHours
                  + checkTimedeltaHours
                  + config.exchange.candleTimedeltaHours,
                ChronoUnit.HOURS
              ),
              interval = CandleInterval.CANDLE_INTERVAL_5_MIN
            ).headOption match {
            case Some(candle) => (Some(candle.getClose), Some(candle.getTime))
            case None => (None, None)
          }

          ShareWrapper(s, s.startingPrice, currentPrice, updateTime)
        }
      )
  }
}
