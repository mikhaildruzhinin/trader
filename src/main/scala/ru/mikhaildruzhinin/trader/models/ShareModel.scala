package ru.mikhaildruzhinin.trader.models

import com.google.protobuf.Timestamp
import ru.mikhaildruzhinin.trader.config.{AppConfig, TypeCode}
import ru.mikhaildruzhinin.trader.database.tables.impl.ShareDAOImpl.InputShareRow
import ru.tinkoff.piapi.contract.v1.Quotation
import ru.tinkoff.piapi.core.utils.DateUtils._
import ru.tinkoff.piapi.core.utils.MapperUtils._

import scala.math.BigDecimal.{RoundingMode, javaBigDecimal2bigDecimal}

case class ShareModel private(figi: String,
                              lot: Int,
                              quantity: Option[Int],
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
      case (Some(s), Some(c)) =>
        Some(
          (quotationToBigDecimal(c) - quotationToBigDecimal(s))
            / quotationToBigDecimal(s)
            * 100 * (100 - appConfig.shares.incomeTaxPct) / 100
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
      case (Some(u), Some(s)) =>
        Some(u * quotationToBigDecimal(s) * lot / 100)
      case _ => None
    }
  }

  lazy val roi: Option[BigDecimal] = {
    val roiNoTax = (purchasePrice, currentPrice) match {
      case (Some(p), Some(c)) =>
        Some((quotationToBigDecimal(c) - quotationToBigDecimal(p)) / quotationToBigDecimal(p) * 100)
      case _ => None
    }

    roiNoTax match {
      case Some(x) if x > 0 => Some(applyTaxes(x))
      case Some(x) if x <= 0 => Some(x)
      case _ => None
    }
  }

  lazy val profit: Option[BigDecimal] = {
    (roi, purchasePrice, quantity) match {
      case (Some(r), Some(p), Some(q)) =>
        Some(r * quotationToBigDecimal(p) * lot * q / 100)
      case _ => None
    }
  }

  private def applyTaxes(noTaxValue: BigDecimal): BigDecimal = {
    noTaxValue * (100 - appConfig.shares.incomeTaxPct) / 100
  }

  def toInputShareRow(typeCode: TypeCode): InputShareRow = (
    updateTime match {
      case Some(t) =>
        Some(java.sql.Timestamp.from(timestampToInstant(t)))
      case _ => None
    },
    lot,
    quantity,
    typeCode.code.toShort,
    appConfig.testFlg,
    figi,
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
    uptrendPct,
    uptrendAbs,
    roi,
    profit
  )

  override def toString: String = {

    new StringBuilder(s"Share info:\n\tName: $name\n\tRoi: ")
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
      .append(" rub.\n\tQuantity: ")
      .append(quantity.getOrElse(0))
      .append("\n\tUpdate time: ")
      .append(timestampToString(updateTime.getOrElse(Timestamp.newBuilder.build)))
      .toString
  }
}

object ShareModel {
  def builder()(implicit appConfig: AppConfig): ShareModelBuilder[Empty] = new ShareModelBuilder()
}
