package ru.mikhaildruzhinin.trader.core.wrappers

import com.google.protobuf.Timestamp
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.mikhaildruzhinin.trader.database.Models.ShareModel
import ru.tinkoff.piapi.contract.v1.{Quotation, Share}
import ru.tinkoff.piapi.core.utils.DateUtils.instantToTimestamp
import ru.tinkoff.piapi.core.utils.MapperUtils.bigDecimalToQuotation

sealed trait ShareWrapperBuilderState
sealed trait Empty extends ShareWrapperBuilderState
sealed trait Figi extends ShareWrapperBuilderState
sealed trait Lot extends ShareWrapperBuilderState
sealed trait Currency extends ShareWrapperBuilderState
sealed trait Name extends ShareWrapperBuilderState
sealed trait Exchange extends ShareWrapperBuilderState

class ShareWrapperBuilder[State <: ShareWrapperBuilderState] private[wrappers](figi: String = "empty",
                                                                               lot: Int = -1,
                                                                               currency: String = "empty",
                                                                               name: String = "empty",
                                                                               exchange: String = "empty",
                                                                               startingPrice: Option[Quotation] = None,
                                                                               purchasePrice: Option[Quotation] = None,
                                                                               currentPrice: Option[Quotation] = None,
                                                                               updateTime: Option[Timestamp] = None)
                                                                              (implicit appConfig: AppConfig) {

  type FullShareWrapper = Empty with Figi with Lot with Currency with Name with Exchange

  private def copy(figi: String = this.figi,
                   lot: Int = this.lot,
                   currency: String = this.currency,
                   name: String = this.name,
                   exchange: String = this.exchange,
                   startingPrice: Option[Quotation] = this.startingPrice,
                   purchasePrice: Option[Quotation] = this.purchasePrice,
                   currentPrice: Option[Quotation] = this.currentPrice,
                   updateTime: Option[Timestamp] = this.updateTime): ShareWrapperBuilder[State] = new ShareWrapperBuilder(
    figi,
    lot,
    currency,
    name,
    exchange,
    startingPrice,
    purchasePrice,
    currentPrice,
    updateTime
  )

  def fromWrapper(shareWrapper: ShareWrapper): ShareWrapperBuilder[Empty
    with Figi
    with Lot
    with Currency
    with Name
    with Exchange
  ] = new ShareWrapperBuilder(
    shareWrapper.figi,
    shareWrapper.lot,
    shareWrapper.currency,
    shareWrapper.name,
    shareWrapper.exchange,
    shareWrapper.startingPrice,
    shareWrapper.purchasePrice,
    shareWrapper.currentPrice,
    shareWrapper.updateTime
  )

  def fromShare(share: Share): ShareWrapperBuilder[Empty
    with Figi
    with Lot
    with Currency
    with Name
    with Exchange
  ] = new ShareWrapperBuilder(
    figi = share.getFigi,
    lot = share.getLot,
    currency = share.getCurrency,
    name = share.getName,
    exchange = share.getExchange
  )

  def fromModel(shareModel: ShareModel): ShareWrapperBuilder[Empty
    with Figi
    with Lot
    with Currency
    with Name
    with Exchange
  ] = {

    val startingPrice = shareModel.startingPrice match {
      case Some(s) => Some(bigDecimalToQuotation(s.bigDecimal))
      case _ => None
    }
    val purchasePrice = shareModel.purchasePrice match {
      case Some(p) => Some(bigDecimalToQuotation(p.bigDecimal))
      case _ => None
    }
    val currentPrice = shareModel.currentPrice match {
      case Some(p) => Some(bigDecimalToQuotation(p.bigDecimal))
      case _ => None
    }
    val updateTime = shareModel.updateDttm match {
      case Some(t) => Some(instantToTimestamp(t))
      case _ => None
    }

    new ShareWrapperBuilder(
      shareModel.figi,
      shareModel.lot,
      shareModel.currency,
      shareModel.name,
      shareModel.exchange,
      startingPrice,
      purchasePrice,
      currentPrice,
      updateTime
    )
  }

  def withStartingPrice(startingPrice: Option[Quotation]): ShareWrapperBuilder[State] = copy(
    startingPrice = startingPrice
  )

  def withPurchasePrice(purchasePrice: Option[Quotation]): ShareWrapperBuilder[State] = copy(
    purchasePrice = purchasePrice
  )

  def withCurrentPrice(currentPrice: Option[Quotation]): ShareWrapperBuilder[State] = copy(
    currentPrice = currentPrice
  )

  def withUpdateTime(updateTime: Option[Timestamp]): ShareWrapperBuilder[State] = copy(
    updateTime = updateTime
  )

  def build()(implicit ev: State =:= FullShareWrapper): ShareWrapper = ShareWrapper(
    figi,
    lot,
    currency,
    name,
    exchange,
    startingPrice,
    purchasePrice,
    currentPrice,
    updateTime
  )
}
