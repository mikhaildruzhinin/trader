package ru.mikhaildruzhinin.trader.models

import com.google.protobuf.Timestamp
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.tinkoff.piapi.contract.v1.{Quotation, Share}
import ru.tinkoff.piapi.core.utils.DateUtils.instantToTimestamp
import ru.tinkoff.piapi.core.utils.MapperUtils.bigDecimalToQuotation

sealed trait ShareModelBuilderState
sealed trait Empty extends ShareModelBuilderState
sealed trait Figi extends ShareModelBuilderState
sealed trait Lot extends ShareModelBuilderState
//sealed trait Quantity extends ShareModelBuilderState
sealed trait Currency extends ShareModelBuilderState
sealed trait Name extends ShareModelBuilderState
sealed trait Exchange extends ShareModelBuilderState

class ShareModelBuilder[State <: ShareModelBuilderState] private[models](figi: String = "empty",
                                                                         lot: Int = -1,
                                                                         quantity: Option[Int] = None,
                                                                         currency: String = "empty",
                                                                         name: String = "empty",
                                                                         exchange: String = "empty",
                                                                         startingPrice: Option[Quotation] = None,
                                                                         purchasePrice: Option[Quotation] = None,
                                                                         currentPrice: Option[Quotation] = None,
                                                                         updateTime: Option[Timestamp] = None)
                                                                        (implicit appConfig: AppConfig) {

  private type FullShareModel = Empty with Figi with Lot /*with Quantity*/ with Currency with Name with Exchange

  private def copy(figi: String = this.figi,
                   lot: Int = this.lot,
                   quantity: Option[Int] = this.quantity,
                   currency: String = this.currency,
                   name: String = this.name,
                   exchange: String = this.exchange,
                   startingPrice: Option[Quotation] = this.startingPrice,
                   purchasePrice: Option[Quotation] = this.purchasePrice,
                   currentPrice: Option[Quotation] = this.currentPrice,
                   updateTime: Option[Timestamp] = this.updateTime): ShareModelBuilder[State] = new ShareModelBuilder(
    figi,
    lot,
    quantity,
    currency,
    name,
    exchange,
    startingPrice,
    purchasePrice,
    currentPrice,
    updateTime
  )

  def fromModel(shareModel: ShareModel): ShareModelBuilder[Empty
    with Figi
    with Lot
//    with Quantity
    with Currency
    with Name
    with Exchange
  ] = new ShareModelBuilder(
    shareModel.figi,
    shareModel.lot,
    shareModel.quantity,
    shareModel.currency,
    shareModel.name,
    shareModel.exchange,
    shareModel.startingPrice,
    shareModel.purchasePrice,
    shareModel.currentPrice,
    shareModel.updateTime
  )

  def fromShare(share: Share): ShareModelBuilder[Empty
    with Figi
    with Lot
    with Currency
    with Name
    with Exchange
  ] = new ShareModelBuilder(
    figi = share.getFigi,
    lot = share.getLot,
    currency = share.getCurrency,
    name = share.getName,
    exchange = share.getExchange
  )

  def fromRowParams(figi: String,
                    lot: Int,
                    quantity: Option[Int],
                    currency: String,
                    name: String,
                    exchange: String,
                    startingPrice: Option[BigDecimal],
                    purchasePrice: Option[BigDecimal],
                    currentPrice: Option[BigDecimal],
                    exchangeUpdateDttm: Option[java.sql.Timestamp]): ShareModelBuilder[Empty
    with Figi
    with Lot
//    with Quantity
    with Currency
    with Name
    with Exchange
  ] = {
    val convertedStartingPrice: Option[Quotation] = startingPrice match {
      case Some(s) => Some(bigDecimalToQuotation(s.bigDecimal))
      case _ => None
    }
    val convertedPurchasePrice: Option[Quotation] = purchasePrice match {
      case Some(p) => Some(bigDecimalToQuotation(p.bigDecimal))
      case _ => None
    }
    val convertedCurrentPrice: Option[Quotation] = currentPrice match {
      case Some(p) => Some(bigDecimalToQuotation(p.bigDecimal))
      case _ => None
    }
    val convertedExchangeUpdateTime: Option[Timestamp] = exchangeUpdateDttm match {
      case Some(t) => Some(instantToTimestamp(t.toInstant))
      case _ => None
    }

    new ShareModelBuilder(
      figi,
      lot,
      quantity,
      currency,
      name,
      exchange,
      convertedStartingPrice,
      convertedPurchasePrice,
      convertedCurrentPrice,
      convertedExchangeUpdateTime
    )
  }

  def withStartingPrice(startingPrice: Option[Quotation]): ShareModelBuilder[State] = copy(
    startingPrice = startingPrice
  )

  def withPurchasePrice(purchasePrice: Option[Quotation]): ShareModelBuilder[State] = copy(
    purchasePrice = purchasePrice
  )

  def withCurrentPrice(currentPrice: Option[Quotation]): ShareModelBuilder[State] = copy(
    currentPrice = currentPrice
  )

  def withUpdateTime(updateTime: Option[Timestamp]): ShareModelBuilder[State] = copy(
    updateTime = updateTime
  )

  def withQuantity(quantity: Option[Int]): ShareModelBuilder[State] = copy(
    quantity = quantity
  )

  def build()(implicit ev: State =:= FullShareModel): ShareModel = ShareModel(
    figi,
    lot,
    quantity,
    currency,
    name,
    exchange,
    startingPrice,
    purchasePrice,
    currentPrice,
    updateTime
  )
}
