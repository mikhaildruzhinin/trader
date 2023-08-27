package ru.mikhaildruzhinin.trader.core

import com.google.protobuf.Timestamp
import ru.tinkoff.piapi.contract.v1.{Account, HistoricCandle, LastPrice, Quotation}

package object dto {
  case class AccountDTO(id: String)

  object AccountDTO {
    def apply(account: Account) = new AccountDTO(account.getId)
  }

  case class HistoricCandleDTO(open: Option[Quotation],
                               close: Option[Quotation],
                               time: Option[Timestamp])

  object HistoricCandleDTO {
    def apply(historicCandle: Option[HistoricCandle]): HistoricCandleDTO = historicCandle match {
      case Some(c) => new HistoricCandleDTO(
        Some(c.getOpen),
        Some(c.getClose),
        Some(c.getTime)
      )
      case None => HistoricCandleDTO(None, None, None)
    }
  }

  case class PriceDTO(price: Quotation,
                      updateTime: Timestamp)

  object PriceDTO {
    def apply(lastPrice: LastPrice) = new PriceDTO(lastPrice.getPrice, lastPrice.getTime)
  }
}
