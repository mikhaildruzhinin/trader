package ru.mikhaildruzhinin.trader.core.dto

import com.google.protobuf.Timestamp
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.tinkoff.piapi.contract.v1.{Quotation, Share}
import ru.tinkoff.piapi.core.utils.DateUtils.instantToTimestamp
import ru.tinkoff.piapi.core.utils.MapperUtils.bigDecimalToQuotation

sealed trait ShareDTOBuilderState
sealed trait Empty extends ShareDTOBuilderState
sealed trait Figi extends ShareDTOBuilderState
sealed trait Lot extends ShareDTOBuilderState
//sealed trait Quantity extends ShareDTOBuilderState
sealed trait Currency extends ShareDTOBuilderState
sealed trait Name extends ShareDTOBuilderState
sealed trait Exchange extends ShareDTOBuilderState

class ShareDTOBuilder[State <: ShareDTOBuilderState] private[dto](figi: String = "empty",
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

  private type FullShareWrapper = Empty with Figi with Lot /*with Quantity*/ with Currency with Name with Exchange

  private def copy(figi: String = this.figi,
                   lot: Int = this.lot,
                   quantity: Option[Int] = this.quantity,
                   currency: String = this.currency,
                   name: String = this.name,
                   exchange: String = this.exchange,
                   startingPrice: Option[Quotation] = this.startingPrice,
                   purchasePrice: Option[Quotation] = this.purchasePrice,
                   currentPrice: Option[Quotation] = this.currentPrice,
                   updateTime: Option[Timestamp] = this.updateTime): ShareDTOBuilder[State] = new ShareDTOBuilder(
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

  def fromDTO(shareDTO: ShareDTO): ShareDTOBuilder[Empty
    with Figi
    with Lot
//    with Quantity
    with Currency
    with Name
    with Exchange
  ] = new ShareDTOBuilder(
    shareDTO.figi,
    shareDTO.lot,
    shareDTO.quantity,
    shareDTO.currency,
    shareDTO.name,
    shareDTO.exchange,
    shareDTO.startingPrice,
    shareDTO.purchasePrice,
    shareDTO.currentPrice,
    shareDTO.updateTime
  )

  def fromShare(share: Share): ShareDTOBuilder[Empty
    with Figi
    with Lot
    with Currency
    with Name
    with Exchange
  ] = new ShareDTOBuilder(
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
                    exchangeUpdateDttm: Option[java.sql.Timestamp]): ShareDTOBuilder[Empty
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

    new ShareDTOBuilder(
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

  def withStartingPrice(startingPrice: Option[Quotation]): ShareDTOBuilder[State] = copy(
    startingPrice = startingPrice
  )

  def withPurchasePrice(purchasePrice: Option[Quotation]): ShareDTOBuilder[State] = copy(
    purchasePrice = purchasePrice
  )

  def withCurrentPrice(currentPrice: Option[Quotation]): ShareDTOBuilder[State] = copy(
    currentPrice = currentPrice
  )

  def withUpdateTime(updateTime: Option[Timestamp]): ShareDTOBuilder[State] = copy(
    updateTime = updateTime
  )

  def withQuantity(quantity: Option[Int]): ShareDTOBuilder[State] = copy(
    quantity = quantity
  )

  def build()(implicit ev: State =:= FullShareWrapper): ShareDTO = ShareDTO(
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
