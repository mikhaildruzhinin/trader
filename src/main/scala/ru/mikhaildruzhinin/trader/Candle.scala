package ru.mikhaildruzhinin.trader

import com.google.protobuf.Timestamp
import ru.tinkoff.piapi.contract.v1.{HistoricCandle, Quotation}

case class Candle(open: Quotation,
                  high: Quotation,
                  low: Quotation,
                  close: Quotation,
                  volume: Long,
                  time: Timestamp,
                  isComplete: Boolean)

object Candle {

  def apply(historicCandle: HistoricCandle): Candle = Candle(
    open = historicCandle.getOpen,
    high = historicCandle.getHigh,
    low = historicCandle.getLow,
    close = historicCandle.getClose,
    volume = historicCandle.getVolume,
    time = historicCandle.getTime,
    isComplete = historicCandle.getIsComplete
  )
}
