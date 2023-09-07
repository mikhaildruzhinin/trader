package ru.mikhaildruzhinin.trader

import com.google.protobuf.Timestamp
import ru.tinkoff.piapi.contract.v1.{Account, HistoricCandle, LastPrice, Quotation}

package object models {
  case class AccountModel(id: String)

  object AccountModel {
    def apply(account: Account) = new AccountModel(account.getId)
  }

  case class CandleModel(open: Option[Quotation],
                         close: Option[Quotation],
                         time: Option[Timestamp])

  object CandleModel {
    def apply(historicCandle: Option[HistoricCandle]): CandleModel = historicCandle match {
      case Some(c) => new CandleModel(
        Some(c.getOpen),
        Some(c.getClose),
        Some(c.getTime)
      )
      case None => CandleModel(None, None, None)
    }
  }

  case class PriceModel(price: Quotation,
                        updateTime: Timestamp)

  object PriceModel {
    def apply(lastPrice: LastPrice) = new PriceModel(lastPrice.getPrice, lastPrice.getTime)
  }
}
