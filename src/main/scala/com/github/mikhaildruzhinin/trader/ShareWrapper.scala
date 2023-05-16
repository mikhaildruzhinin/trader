package com.github.mikhaildruzhinin.trader

import ru.tinkoff.piapi.contract.v1.{Quotation, Share}
import ru.tinkoff.piapi.core.utils.MapperUtils.quotationToBigDecimal

import scala.math.BigDecimal.{RoundingMode, javaBigDecimal2bigDecimal}

case class ShareWrapper(figi: String,
                        lot: Int,
                        currency: String,
                        name: String,
                        exchange: String,
                        openPrice: Option[Quotation] = None,
                        closePrice: Option[Quotation] = None)
                       (implicit config: Config) {

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
