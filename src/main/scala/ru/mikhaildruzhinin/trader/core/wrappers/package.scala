package ru.mikhaildruzhinin.trader.core

import com.google.protobuf.Timestamp
import ru.tinkoff.piapi.contract.v1.{Account, HistoricCandle, LastPrice, Quotation}

package object wrappers {
  case class AccountWrapper(id: String)

  object AccountWrapper {
    def apply(account: Account) = new AccountWrapper(account.getId)
  }

  case class HistoricCandleWrapper(open: Option[Quotation],
                                   close: Option[Quotation],
                                   time: Option[Timestamp])

  object HistoricCandleWrapper {
    def apply(historicCandle: Option[HistoricCandle]): HistoricCandleWrapper = historicCandle match {
      case Some(c) => new HistoricCandleWrapper(
        Some(c.getOpen),
        Some(c.getClose),
        Some(c.getTime)
      )
      case None => HistoricCandleWrapper(None, None, None)
    }



  }

  case class PriceWrapper(price: Quotation,
                          updateTime: Timestamp)

  object PriceWrapper {
    def apply(lastPrice: LastPrice) = new PriceWrapper(lastPrice.getPrice, lastPrice.getTime)
  }
}
