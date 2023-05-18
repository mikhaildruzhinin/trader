package com.github.mikhaildruzhinin.trader

import com.google.protobuf.Timestamp
import ru.tinkoff.piapi.contract.v1.{Quotation, Share}
import ru.tinkoff.piapi.core.utils.DateUtils.timestampToString
import ru.tinkoff.piapi.core.utils.MapperUtils.quotationToBigDecimal

import scala.math.BigDecimal.{RoundingMode, javaBigDecimal2bigDecimal}

case class ShareWrapper(figi: String,
                        lot: Int,
                        currency: String,
                        name: String,
                        exchange: String,
                        openPrice: Option[Quotation] = None,
                        closePrice: Option[Quotation] = None,
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
           openPrice: Option[Quotation],
           closePrice: Option[Quotation],
           updateTime: Option[Timestamp])
          (implicit config: Config) = this(
    shareWrapper.figi,
    shareWrapper.lot,
    shareWrapper.currency,
    shareWrapper.name,
    shareWrapper.exchange,
    openPrice,
    closePrice,
    updateTime
  )

  lazy val uptrendPct: Option[BigDecimal] = {
    (openPrice, closePrice) match {
      case (Some(openPriceValue), Some(closePriceValue)) =>
        Some(
          (
            (quotationToBigDecimal(closePriceValue) - quotationToBigDecimal(openPriceValue))
              / quotationToBigDecimal(openPriceValue) * 100
          ).setScale(config.pctScale, RoundingMode.HALF_UP)
        )
      case _ => None
    }
  }

  lazy val uptrendAbsNoTax: Option[BigDecimal] = {
    (openPrice, uptrendPct) match {
      case (Some(openPriceValue), Some(uptrendPctValue)) =>
        Some(
          (quotationToBigDecimal(openPriceValue) * lot * uptrendPctValue / 100)
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
    closePrice match {
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
  def apply(share: Share)
           (implicit config: Config): ShareWrapper = {
    new ShareWrapper(share)
  }

  def apply(shareWrapper: ShareWrapper,
            openPrice: Option[Quotation],
            closePrice: Option[Quotation],
            updateTime: Option[Timestamp])
           (implicit config: Config): ShareWrapper = {
    new ShareWrapper(shareWrapper, openPrice, closePrice, updateTime)
  }
}
