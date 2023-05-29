package com.github.mikhaildruzhinin.trader

import com.github.mikhaildruzhinin.trader.config.AppConfig
import com.github.mikhaildruzhinin.trader.database.Models
import com.github.mikhaildruzhinin.trader.database.Models.ShareType
import com.google.protobuf.Timestamp
import ru.tinkoff.piapi.contract.v1.{CandleInterval, LastPrice, Quotation, Share}
import ru.tinkoff.piapi.core.utils.DateUtils.{instantToTimestamp, timestampToInstant, timestampToString}
import ru.tinkoff.piapi.core.utils.MapperUtils.{bigDecimalToQuotation, quotationToBigDecimal}

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

  def this(share: Models.Share)
          (implicit appConfig: AppConfig) = this(
    share.figi,
    share.lot,
    share.currency,
    share.name,
    share.exchange,
    share.startingPrice match {
      case Some(s) => Some(bigDecimalToQuotation(s.bigDecimal))
      case _ => None
    },
    share.purchasePrice match {
      case Some(p) => Some(bigDecimalToQuotation(p.bigDecimal))
      case _ => None
    },
    share.currentPrice match {
      case Some(p) => Some(bigDecimalToQuotation(p.bigDecimal))
      case _ => None
    },
    share.updateDttm match {
      case Some(t) => Some(instantToTimestamp(t))
      case _ => None
    }
  )

  lazy val uptrendPct: Option[BigDecimal] = {
    val uptrendPctNoTax: Option[BigDecimal] = (startingPrice, currentPrice) match {
      case (Some(startingPriceValue), Some(currentPriceValue)) =>
        Some(
          (
            quotationToBigDecimal(currentPriceValue)
              - quotationToBigDecimal(startingPriceValue)
          )
            / quotationToBigDecimal(startingPriceValue)
            * 100
            * (100 - appConfig.incomeTaxPct) / 100
        )
      case _ => None
    }

    uptrendPctNoTax match {
      case Some(x) if x > 0 => Some(applyTaxes(x))
      case Some(x) if x <= 0 => Some(x)
      case _ => None
    }
  }

  lazy val uptrendAbs: Option[BigDecimal] = {
    (uptrendPct, startingPrice) match {
      case (Some(uptrendPctValue), Some(startingPriceValue)) =>
        Some(uptrendPctValue * quotationToBigDecimal(startingPriceValue) * lot / 100)
      case _ => None
    }
  }

  lazy val roi: Option[BigDecimal] = {
    val roiNoTax = (purchasePrice, currentPrice) match {
      case (Some(purchasePriceValue), Some(currentPriceValue)) =>
        Some(
          (quotationToBigDecimal(currentPriceValue) - quotationToBigDecimal(purchasePriceValue))
            / quotationToBigDecimal(purchasePriceValue) * 100
        )
      case _ => None
    }

    roiNoTax match {
      case Some(x) if x > 0 => Some(applyTaxes(x))
      case Some(x) if x <= 0 => Some(x)
      case _ => None
    }
  }

  lazy val isCheaperThanPurchasePrice: Boolean = {
    quotationToBigDecimal(
      currentPrice.getOrElse(Quotation.newBuilder.build)
    ) < quotationToBigDecimal(
      purchasePrice.getOrElse(Quotation.newBuilder.build)
    )
  }

  private def applyTaxes(noTaxValue: BigDecimal): BigDecimal = {
    noTaxValue * (100 - appConfig.incomeTaxPct) / 100
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

  def getShareTuple(typeCd: Int): ShareType = (
    typeCd,
    figi,
    lot,
    currency,
    name,
    exchange,
    startingPrice match {
      case Some(s) =>
        Some(quotationToBigDecimal(s)
          .setScale(appConfig.priceScale, RoundingMode.HALF_UP))
      case _ => None
    },
    purchasePrice match {
      case Some(p) =>
        Some(quotationToBigDecimal(p)
          .setScale(appConfig.priceScale, RoundingMode.HALF_UP))
      case _ => None
    },
    currentPrice match {
      case Some(p) =>
        Some(quotationToBigDecimal(p)
          .setScale(appConfig.priceScale, RoundingMode.HALF_UP))
      case _ => None
    },
    updateTime match {
      case Some(t) =>
        Some(timestampToInstant(t))
      case _ => None
    },
    appConfig.testFlg
  )

  override def toString: String = {
    val a: BigDecimal = roi.getOrElse(BigDecimal(0))

    new StringBuilder(s"$name, ")
      .append(lot)
      .append("шт., ")
      .append(roi
        .getOrElse(BigDecimal(0))
        .setScale(appConfig.pctScale, RoundingMode.HALF_UP))
      .append("%, ")
      .append((quotationToBigDecimal(currentPrice.getOrElse(Quotation.newBuilder.build)) * lot)
        .setScale(appConfig.priceScale, RoundingMode.HALF_UP))
      .append(" руб., ")
      .append(uptrendPct
        .getOrElse(BigDecimal(-1))
        .setScale(appConfig.priceScale, RoundingMode.HALF_UP))
      .append("%, ")
      .append(uptrendAbs
        .getOrElse(BigDecimal(-1))
        .setScale(appConfig.pctScale, RoundingMode.HALF_UP)
      )
      .append(" руб., ")
      .append(timestampToString(updateTime.getOrElse(Timestamp.newBuilder.build)))
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

  def apply(shareWrapper: ShareWrapper,
            lastPrice: LastPrice)
           (implicit appConfig: AppConfig): ShareWrapper = new ShareWrapper(shareWrapper, lastPrice)

  def apply(share: Models.Share)
           (implicit appConfig: AppConfig): ShareWrapper = new ShareWrapper(share)

  private def getFilteredShares(implicit appConfig: AppConfig,
                                investApiClient: InvestApiClient.type): List[Share] = {

    val shares: List[Share] = investApiClient
      .getShares
      .filter(
        s =>
          appConfig.exchange.names.contains(s.getExchange)
            && s.getApiTradeAvailableFlag
      )

    LocalDate.now.getDayOfWeek match {
      case DayOfWeek.SATURDAY => shares.filter(_.getWeekendFlag)
      case DayOfWeek.SUNDAY => shares.filter(_.getWeekendFlag)
      case _ => shares
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
                         investApiClient: InvestApiClient.type): Seq[ShareWrapper] = {

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
