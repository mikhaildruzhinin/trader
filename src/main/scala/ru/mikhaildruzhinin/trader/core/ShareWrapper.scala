package ru.mikhaildruzhinin.trader.core

import com.google.protobuf.Timestamp
import com.typesafe.scalalogging.Logger
import ru.mikhaildruzhinin.trader.client.BaseInvestApiClient
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.mikhaildruzhinin.trader.database.Models.{ShareModel, ShareType}
import ru.mikhaildruzhinin.trader.database.connection.Connection
import ru.mikhaildruzhinin.trader.database.tables.SharesTable
import ru.tinkoff.piapi.contract.v1._
import ru.tinkoff.piapi.core.utils.DateUtils._
import ru.tinkoff.piapi.core.utils.MapperUtils._

import java.time.{DayOfWeek, Instant, LocalDate}
import scala.math.BigDecimal.{RoundingMode, javaBigDecimal2bigDecimal}

case class ShareWrapper private (figi: String,
                        lot: Int,
                        currency: String,
                        name: String,
                        exchange: String,
                        startingPrice: scala.Option[Quotation] = None,
                        purchasePrice: scala.Option[Quotation] = None,
                        currentPrice: scala.Option[Quotation] = None,
                        updateTime: scala.Option[Timestamp] = None)
                       (implicit appConfig: AppConfig) {

  private def this(share: Share)
          (implicit appConfig: AppConfig) = this(
    share.getFigi,
    share.getLot,
    share.getCurrency,
    share.getName,
    share.getExchange
  )

  private def this(shareWrapper: ShareWrapper,
           startingPrice: scala.Option[Quotation],
           purchasePrice: scala.Option[Quotation],
           currentPrice: scala.Option[Quotation],
           updateTime: scala.Option[Timestamp])
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

  private def this(shareWrapper: ShareWrapper,
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

  private def this(share: ShareModel)
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

  lazy val uptrendPct: scala.Option[BigDecimal] = {
    val uptrendPctNoTax: scala.Option[BigDecimal] = (startingPrice, currentPrice) match {
      case (Some(startingPriceValue), Some(currentPriceValue)) =>
        Some(
          (
            quotationToBigDecimal(currentPriceValue)
              - quotationToBigDecimal(startingPriceValue)
          )
            / quotationToBigDecimal(startingPriceValue)
            * 100
            * (100 - appConfig.shares.incomeTaxPct) / 100
        )
      case _ => None
    }

    uptrendPctNoTax match {
      case Some(x) if x > 0 => Some(applyTaxes(x))
      case Some(x) if x <= 0 => Some(x)
      case _ => None
    }
  }

  lazy val uptrendAbs: scala.Option[BigDecimal] = {
    (uptrendPct, startingPrice) match {
      case (Some(uptrendPctValue), Some(startingPriceValue)) =>
        Some(uptrendPctValue * quotationToBigDecimal(startingPriceValue) * lot / 100)
      case _ => None
    }
  }

  lazy val roi: scala.Option[BigDecimal] = {
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

  lazy val profit: scala.Option[BigDecimal] = {
    (roi, purchasePrice) match {
      case (Some(r), Some(p)) =>
        Some(r * quotationToBigDecimal(p) * lot / 100)
      case _ => None
    }
  }

  private def applyTaxes(noTaxValue: BigDecimal): BigDecimal = {
    noTaxValue * (100 - appConfig.shares.incomeTaxPct) / 100
  }

  def updateShare(implicit appConfig: AppConfig,
                  investApiClient: BaseInvestApiClient): ShareWrapper = {

    val (
      _: scala.Option[Quotation],
      currentPrice: scala.Option[Quotation],
      updateTime: scala.Option[Timestamp]
    ) = ShareWrapper.getUpdatedCandlePrices(
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

  def toShareType(typeCode: TypeCode): ShareType = (
    TypeCode.unapply(typeCode),
    figi,
    lot,
    currency,
    name,
    exchange,
    startingPrice match {
      case Some(s) =>
        Some(quotationToBigDecimal(s)
          .setScale(appConfig.shares.priceScale, RoundingMode.HALF_UP))
      case _ => None
    },
    purchasePrice match {
      case Some(p) =>
        Some(quotationToBigDecimal(p)
          .setScale(appConfig.shares.priceScale, RoundingMode.HALF_UP))
      case _ => None
    },
    currentPrice match {
      case Some(p) =>
        Some(quotationToBigDecimal(p)
          .setScale(appConfig.shares.priceScale, RoundingMode.HALF_UP))
      case _ => None
    },
    updateTime match {
      case Some(t) =>
        Some(timestampToInstant(t))
      case _ => None
    },
    uptrendPct,
    uptrendAbs,
    roi,
    profit,
    appConfig.testFlg
  )

  override def toString: String = {

    new StringBuilder(s"$name, ")
      .append(roi
        .getOrElse(BigDecimal(0))
        .setScale(appConfig.shares.pctScale, RoundingMode.HALF_UP))
      .append("%, ")
      .append((quotationToBigDecimal(purchasePrice.getOrElse(Quotation.newBuilder.build)) * lot)
        .setScale(appConfig.shares.priceScale, RoundingMode.HALF_UP))
      .append(" руб., ")
      .append((quotationToBigDecimal(currentPrice.getOrElse(Quotation.newBuilder.build)) * lot)
        .setScale(appConfig.shares.priceScale, RoundingMode.HALF_UP))
      .append(" руб., ")
      .append(profit.getOrElse(BigDecimal(0))
        .setScale(appConfig.shares.priceScale, RoundingMode.HALF_UP))
      .append(" руб., ")
      .append(timestampToString(updateTime.getOrElse(Timestamp.newBuilder.build)))
      .toString
  }
}

object ShareWrapper {
  val log: Logger = Logger(getClass.getName)

  def apply(share: Share)
           (implicit appConfig: AppConfig): ShareWrapper = new ShareWrapper(share)

  def apply(shareWrapper: ShareWrapper,
            startingPrice: scala.Option[Quotation],
            purchasePrice: scala.Option[Quotation],
            currentPrice: scala.Option[Quotation],
            updateTime: scala.Option[Timestamp])
           (implicit appConfig: AppConfig): ShareWrapper = {

    new ShareWrapper(shareWrapper, startingPrice, purchasePrice, currentPrice, updateTime)
  }

  def apply(shareWrapper: ShareWrapper,
            lastPrice: LastPrice)
           (implicit appConfig: AppConfig): ShareWrapper = new ShareWrapper(shareWrapper, lastPrice)

  def apply(share: ShareModel)
           (implicit appConfig: AppConfig): ShareWrapper = new ShareWrapper(share)

  def getUpdatedCandlePrices(shareWrapper: ShareWrapper,
                             from: Instant,
                             to: Instant,
                             interval: CandleInterval)
                            (implicit appConfig: AppConfig,
                             investApiClient: BaseInvestApiClient): (
    scala.Option[Quotation],
      scala.Option[Quotation],
      scala.Option[Timestamp]
    ) = {

    val candle: scala.Option[HistoricCandle] = investApiClient
      .getCandles(shareWrapper.figi, from, to, interval)
      .headOption

    candle match {
        case Some(c) => (
          Some(c.getOpen),
          Some(c.getClose),
          Some(c.getTime)
        )
        case None => (None, None, None)
      }
  }

  def getPersistedShares(typeCode: TypeCode)
                        (implicit appConfig: AppConfig,
                         connection: Connection): Seq[ShareWrapper] = {

    val code = TypeCode.unapply(typeCode)
    val shares: Seq[ShareWrapper] = connection
      .runMultiple(
        Vector(SharesTable.filterByTypeCode(code))
      )
      .flatten
      .map(s => ShareWrapper(s))

    log.info(s"Got ${shares.length} shares of type: $typeCode($code)")
    shares
  }
}
