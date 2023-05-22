package com.github.mikhaildruzhinin.trader

import com.github.mikhaildruzhinin.trader.config.Config
import com.google.protobuf.Timestamp
import ru.tinkoff.piapi.contract.v1.{CandleInterval, Quotation, Share}
import ru.tinkoff.piapi.core.utils.DateUtils.timestampToString
import ru.tinkoff.piapi.core.utils.MapperUtils.quotationToBigDecimal

import java.time.{DayOfWeek, Instant, LocalDate}
import scala.math.BigDecimal.{RoundingMode, javaBigDecimal2bigDecimal}

case class ShareWrapper(figi: String,
                        lot: Int,
                        currency: String,
                        name: String,
                        exchange: String,
                        startingPrice: Option[Quotation] = None,
                        purchasePrice: Option[Quotation] = None,
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
           purchasePrice: Option[Quotation],
           currentPrice: Option[Quotation],
           updateTime: Option[Timestamp])
          (implicit config: Config) = this(
    shareWrapper.figi,
    shareWrapper.lot,
    shareWrapper.currency,
    shareWrapper.name,
    shareWrapper.exchange,
    startingPrice,
    purchasePrice,
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

  def updateShare(implicit config: Config,
                  investApiClient: InvestApiClient.type): ShareWrapper = {

    val (_: Option[Quotation], currentPrice: Option[Quotation], updateTime: Option[Timestamp]) = ShareWrapper.getUpdatedCandlePrices(
      shareWrapper = this,
      from = config.exchange.updateInstantFrom,
      to = config.exchange.updateInstantTo,
      interval = CandleInterval.CANDLE_INTERVAL_5_MIN
    )

    ShareWrapper(
      this,
      startingPrice,
      None,
      currentPrice,
      updateTime
    )
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
  def apply(share: Share)
           (implicit config: Config): ShareWrapper = new ShareWrapper(share)

  def apply(shareWrapper: ShareWrapper,
            startingPrice: Option[Quotation],
            purchasePrice: Option[Quotation],
            currentPrice: Option[Quotation],
            updateTime: Option[Timestamp])
           (implicit config: Config): ShareWrapper = {
    new ShareWrapper(shareWrapper, startingPrice, purchasePrice, currentPrice, updateTime)
  }

  private def getFilteredShares(implicit config: Config,
                                investApiClient: InvestApiClient.type): Iterator[Share] = {

    LocalDate.now.getDayOfWeek match {
      case DayOfWeek.SATURDAY => investApiClient.getShares.filter(_.getWeekendFlag)
      case DayOfWeek.SUNDAY => investApiClient.getShares.filter(_.getWeekendFlag)
      case _ => investApiClient.getShares
    }
  }

  private def getUpdatedCandlePrices(shareWrapper: ShareWrapper,
                                     from: Instant,
                                     to: Instant,
                                     interval: CandleInterval)
                                    (implicit config: Config,
                                     investApiClient: InvestApiClient.type): (Option[Quotation], Option[Quotation], Option[Timestamp]) = {

    investApiClient
      .getCandles(shareWrapper, from, to, interval)
      .headOption match {
        case Some(candle) => (Some(candle.getOpen), Some(candle.getClose), Some(candle.getTime))
        case None => (None, None, None)
      }
  }

  def getAvailableShares(implicit config: Config,
                         investApiClient: InvestApiClient.type): Iterator[ShareWrapper] = {

    getFilteredShares
      .map(
        s => {
          val shareWrapper = ShareWrapper(s)

          val (startingPrice: Option[Quotation], _: Option[Quotation], updateTime: Option[Timestamp]) = getUpdatedCandlePrices(
              shareWrapper = shareWrapper,
              from = config.exchange.startInstantFrom,
              to = config.exchange.startInstantTo,
              interval = CandleInterval.CANDLE_INTERVAL_5_MIN
            )

          ShareWrapper(shareWrapper, startingPrice, None, None, updateTime)
        }
      )
  }
}
