package ru.mikhaildruzhinin.trader.core.wrappers

import com.google.protobuf.Timestamp
import ru.tinkoff.piapi.contract.v1.{LastPrice, Quotation}

case class PriceWrapper(lastPrice: LastPrice) {
  lazy val price: Quotation = lastPrice.getPrice
  lazy val updateTime: Timestamp = lastPrice.getTime
}
