package ru.mikhaildruzhinin.trader.core

import com.google.protobuf.Timestamp
import ru.tinkoff.piapi.contract.v1.{HistoricCandle, Quotation}

case class HistoricCandleWrapper(historicCandle: Option[HistoricCandle]) {
  lazy val open: Option[Quotation] = historicCandle match {
    case Some(c) => Some(c.getOpen)
    case _ => None
  }

  lazy val close: Option[Quotation] = historicCandle match {
    case Some(c) => Some(c.getClose)
    case _ => None
  }

  lazy val time: Option[Timestamp] = historicCandle match {
    case Some(c) => Some(c.getTime)
    case _ => None
  }
}
