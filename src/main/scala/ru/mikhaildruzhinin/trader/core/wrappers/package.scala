package ru.mikhaildruzhinin.trader.core

import com.google.protobuf.Timestamp
import ru.tinkoff.piapi.contract.v1.{Account, HistoricCandle, LastPrice, Quotation}

package object wrappers {
  case class AccountWrapper(id: String)

  object AccountWrapper {
    def apply(account: Account) = new AccountWrapper(account.getId)
  }

  case class HistoricCandleWrapper(open: Quotation,
                                   close: Quotation,
                                   time: Timestamp)

  object HistoricCandleWrapper {
    def apply(historicCandle: HistoricCandle) = new HistoricCandleWrapper(
      historicCandle.getOpen,
      historicCandle.getClose,
      historicCandle.getTime
    )
  }

  case class PriceWrapper(price: Quotation,
                          updateTime: Timestamp)

  object PriceWrapper {
    def apply(lastPrice: LastPrice) = new PriceWrapper(lastPrice.getPrice, lastPrice.getTime)
  }
}
