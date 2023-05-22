package com.github.mikhaildruzhinin.trader

import com.github.mikhaildruzhinin.trader.config.AppConfig
import com.google.protobuf.Timestamp
import ru.tinkoff.piapi.contract.v1.{CandleInterval, LastPrice, Quotation, Share}
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
                       (implicit appConfig: AppConfig) {

  def this(share: Share)(implicit appConfig: AppConfig) = this(
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
          (implicit appConfig: AppConfig) = this(
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

  def this(shareWrapper: ShareWrapper,
           lastPrice: LastPrice)
          (implicit appConfig: AppConfig) = this(
    shareWrapper.figi,
    shareWrapper.lot,
    shareWrapper.currency,
    shareWrapper.name,
    shareWrapper.exchange,
    shareWrapper.startingPrice,
    shareWrapper.purchasePrice,
    Some(lastPrice.getPrice),
    Some(lastPrice.getTime)
  )

  lazy val uptrendPct: Option[BigDecimal] = {
    (startingPrice, currentPrice) match {
      case (Some(startingPriceValue), Some(currentPriceValue)) =>
        Some(
          (
            (quotationToBigDecimal(currentPriceValue) - quotationToBigDecimal(startingPriceValue))
              / quotationToBigDecimal(startingPriceValue) * 100
          ).setScale(appConfig.pctScale, RoundingMode.HALF_UP)
        )
      case _ => None
    }
  }

  lazy val uptrendAbs: Option[BigDecimal] = {
    (startingPrice, currentPrice) match {
      case (Some(startingPriceValue), Some(currentPriceValue)) =>
        Some(
          (
            (quotationToBigDecimal(currentPriceValue)
              - quotationToBigDecimal(startingPriceValue))
              * lot * (100 - appConfig.incomeTaxPct) / 100
          ).setScale(appConfig.priceScale, RoundingMode.HALF_UP)
        )
      case _ => None
    }
  }

  def updateShare(implicit appConfig: AppConfig,
                  investApiClient: InvestApiClient.type): ShareWrapper = {

    val (_: Option[Quotation], currentPrice: Option[Quotation], updateTime: Option[Timestamp]) = ShareWrapper.getUpdatedCandlePrices(
      shareWrapper = this,
      from = appConfig.exchange.updateInstantFrom,
      to = appConfig.exchange.updateInstantTo,
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

  def isCheaperThanPurchasePrice: Boolean = {
    quotationToBigDecimal(
      currentPrice.getOrElse(Quotation.newBuilder.build)
    ) < quotationToBigDecimal(
      purchasePrice.getOrElse(Quotation.newBuilder.build)
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
           (implicit appConfig: AppConfig): ShareWrapper = new ShareWrapper(share)

  def apply(shareWrapper: ShareWrapper,
            startingPrice: Option[Quotation],
            purchasePrice: Option[Quotation],
            currentPrice: Option[Quotation],
            updateTime: Option[Timestamp])
           (implicit appConfig: AppConfig): ShareWrapper = {
    new ShareWrapper(shareWrapper, startingPrice, purchasePrice, currentPrice, updateTime)
  }

  def apply(shareWrapper: ShareWrapper, lastPrice: LastPrice)
           (implicit appConfig: AppConfig): ShareWrapper = {
    new ShareWrapper(shareWrapper, lastPrice)
  }

  private def getFilteredShares(implicit appConfig: AppConfig,
                                investApiClient: InvestApiClient.type): List[Share] = {

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
                                    (implicit appConfig: AppConfig,
                                     investApiClient: InvestApiClient.type): (Option[Quotation], Option[Quotation], Option[Timestamp]) = {

    investApiClient
      .getCandles(shareWrapper.figi, from, to, interval)
      .headOption match {
        case Some(candle) => (Some(candle.getOpen), Some(candle.getClose), Some(candle.getTime))
        case None => (None, None, None)
      }
  }

  def getAvailableShares(implicit appConfig: AppConfig,
                         investApiClient: InvestApiClient.type): List[ShareWrapper] = {

    getFilteredShares
      .map(
        s => {
          val shareWrapper = ShareWrapper(s)

          val (startingPrice: Option[Quotation], _: Option[Quotation], updateTime: Option[Timestamp]) = getUpdatedCandlePrices(
              shareWrapper = shareWrapper,
              from = appConfig.exchange.startInstantFrom,
              to = appConfig.exchange.startInstantTo,
              interval = CandleInterval.CANDLE_INTERVAL_5_MIN
            )

          ShareWrapper(shareWrapper, startingPrice, None, None, updateTime)
        }
      )
  }
}
