package ru.mikhaildruzhinin.trader

import com.google.protobuf.Timestamp
import org.ta4j.core.num.DecimalNum
import org.ta4j.core.{Bar, BaseBar}
import ru.mikhaildruzhinin.trader.Candle.{candleIntervalToDuration, quotationToDecimalNum}
import ru.tinkoff.piapi.contract.v1.{CandleInterval, HistoricCandle, Quotation}
import ru.tinkoff.piapi.core.utils.DateUtils.timestampToInstant
import ru.tinkoff.piapi.core.utils.MapperUtils.quotationToBigDecimal

import java.time.temporal.ChronoUnit
import java.time.{Duration, ZoneId}

case class Candle(open: Quotation,
                  high: Quotation,
                  low: Quotation,
                  close: Quotation,
                  volume: Long,
                  time: Timestamp,
                  isComplete: Boolean,
                  candleInterval: CandleInterval) {

  def toBar: Option[Bar] = candleIntervalToDuration(candleInterval) match {
    case Some(timePeriod) => Some(
      BaseBar.builder()
      .timePeriod(timePeriod)
      .openPrice(quotationToDecimalNum(open))
      .highPrice(quotationToDecimalNum(high))
      .lowPrice(quotationToDecimalNum(low))
      .closePrice(quotationToDecimalNum(close))
      .volume(DecimalNum.valueOf(volume))
      .endTime(timestampToInstant(time).atZone(ZoneId.of("UTC")))
      .build()
    )
    case None => None
  }
}

object Candle {

  def apply(historicCandle: HistoricCandle,
            candleInterval: CandleInterval): Candle = Candle(
    open = historicCandle.getOpen,
    high = historicCandle.getHigh,
    low = historicCandle.getLow,
    close = historicCandle.getClose,
    volume = historicCandle.getVolume,
    time = historicCandle.getTime,
    isComplete = historicCandle.getIsComplete,
    candleInterval = candleInterval
  )

  private def quotationToDecimalNum(quotation: Quotation): DecimalNum = DecimalNum
    .valueOf(quotationToBigDecimal(quotation))

  private def candleIntervalToDuration(candleInterval: CandleInterval): Option[Duration] = {
    candleInterval.getNumber match {
      case 0 => None
      case 1 => Some(Duration.of(1, ChronoUnit.MINUTES))
      case 2 => Some(Duration.of(5, ChronoUnit.MINUTES))
      case 3 => Some(Duration.of(15, ChronoUnit.MINUTES))
      case 4 => Some(Duration.of(1, ChronoUnit.HOURS))
      case 5 => Some(Duration.of(1, ChronoUnit.DAYS))
      case 6 => Some(Duration.of(2, ChronoUnit.MINUTES))
      case 7 => Some(Duration.of(3, ChronoUnit.MINUTES))
      case 8 => Some(Duration.of(10, ChronoUnit.MINUTES))
      case 9 => Some(Duration.of(30, ChronoUnit.MINUTES))
      case 10 => Some(Duration.of(2, ChronoUnit.HOURS))
      case 11 => Some(Duration.of(4, ChronoUnit.HOURS))
      case 12 => Some(Duration.of(1, ChronoUnit.WEEKS))
      case 13 => Some(Duration.of(1, ChronoUnit.MONTHS))
      case -1 => None
    }
  }
}
