package ru.mikhaildruzhinin.trader

import org.scalatest.funsuite.AnyFunSuite
import ru.tinkoff.piapi.contract.v1.{CandleInterval, HistoricCandle}

import java.time.{LocalDate, ZoneId}

class InvestApiClientSuite extends AnyFunSuite with Components {
  test("test rate limiter") {
    investApiClient.getCandles(
      "BBG001M2SC01",
      LocalDate.now.atStartOfDay(ZoneId.of("UTC")).plusHours(15).toInstant,
      LocalDate.now.atStartOfDay(ZoneId.of("UTC")).plusHours(16).toInstant,
      CandleInterval.CANDLE_INTERVAL_5_MIN
    )

    (0 to 1000).foreach(i => {
      val r: Seq[HistoricCandle] = investApiClient.getCandles(
        "BBG001M2SC01",
        LocalDate.now.atStartOfDay(ZoneId.of("UTC")).plusHours(15).toInstant,
        LocalDate.now.atStartOfDay(ZoneId.of("UTC")).plusHours(16).toInstant,
        CandleInterval.CANDLE_INTERVAL_5_MIN
      )
      println(r)
    })
  }
}
