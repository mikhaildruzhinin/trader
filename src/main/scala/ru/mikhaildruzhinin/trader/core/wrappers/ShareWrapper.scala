package ru.mikhaildruzhinin.trader.core.wrappers

import com.google.protobuf.Timestamp
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.mikhaildruzhinin.trader.core.TypeCode
import ru.mikhaildruzhinin.trader.database.Models.ShareType
import ru.tinkoff.piapi.contract.v1.Quotation
import ru.tinkoff.piapi.core.utils.DateUtils._
import ru.tinkoff.piapi.core.utils.MapperUtils._

import scala.math.BigDecimal.{RoundingMode, javaBigDecimal2bigDecimal}

case class ShareWrapper private (figi: String,
                                 lot: Int,
                                 currency: String,
                                 name: String,
                                 exchange: String,
                                 startingPrice: Option[Quotation] = None,
                                 purchasePrice: Option[Quotation] = None,
                                 currentPrice: Option[Quotation] = None,
                                 updateTime: Option[Timestamp] = None)
                                 (implicit appConfig: AppConfig) {

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

  lazy val profit: Option[BigDecimal] = {
    (roi, purchasePrice) match {
      case (Some(r), Some(p)) =>
        Some(r * quotationToBigDecimal(p) * lot / 100)
      case _ => None
    }
  }

  private def applyTaxes(noTaxValue: BigDecimal): BigDecimal = {
    noTaxValue * (100 - appConfig.shares.incomeTaxPct) / 100
  }

  def toShareType(typeCode: TypeCode): ShareType = (
    typeCode.code,
    figi,
    lot,
    currency,
    name,
    exchange,
    startingPrice match {
      case Some(s) =>
        Some(quotationToBigDecimal(s))
      case _ => None
    },
    purchasePrice match {
      case Some(p) =>
        Some(quotationToBigDecimal(p))
      case _ => None
    },
    currentPrice match {
      case Some(p) =>
        Some(quotationToBigDecimal(p))
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

    new StringBuilder(s"Sold share:\n\tName: $name\n\tRoi: ")
      .append(roi
        .getOrElse(BigDecimal(0))
        .setScale(appConfig.shares.pctScale, RoundingMode.HALF_UP))
      .append("%\n\tPurchase price: ")
      .append((quotationToBigDecimal(purchasePrice.getOrElse(Quotation.newBuilder.build)) * lot)
        .setScale(appConfig.shares.priceScale, RoundingMode.HALF_UP))
      .append(" rub.\n\tCurrent price: ")
      .append((quotationToBigDecimal(currentPrice.getOrElse(Quotation.newBuilder.build)) * lot)
        .setScale(appConfig.shares.priceScale, RoundingMode.HALF_UP))
      .append(" rub.,\n\tProfit: ")
      .append(profit.getOrElse(BigDecimal(0))
        .setScale(appConfig.shares.priceScale, RoundingMode.HALF_UP))
      .append(" rub.\n\tUpdate time: ")
      .append(timestampToString(updateTime.getOrElse(Timestamp.newBuilder.build)))
      .toString
  }
}

object ShareWrapper {
  def builder()(implicit appConfig: AppConfig): ShareWrapperBuilder[Empty] = new ShareWrapperBuilder()
}
