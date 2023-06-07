package ru.mikhaildruzhinin.trader.config.exchange

import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalDate, ZoneOffset}

case class ExchangeConfig(names: List[String],
                          startTimeHours: Int,
                          uptrendCheckTimedeltaHours: Int,
                          candleTimedeltaHours: Int) {

  private lazy val startDayInstant: Instant = LocalDate
    .now
    .atStartOfDay
    .toInstant(ZoneOffset.UTC)

  lazy val startInstantFrom: Instant = startDayInstant
    .plus(
      startTimeHours,
      ChronoUnit.HOURS
    )

  lazy val startInstantTo: Instant = startDayInstant
    .plus(
      startTimeHours
        + candleTimedeltaHours,
      ChronoUnit.HOURS
    )

  lazy val updateInstantFrom: Instant = startDayInstant
    .plus(
      startTimeHours
        + uptrendCheckTimedeltaHours,
      ChronoUnit.HOURS
    )

  lazy val updateInstantTo: Instant = startDayInstant
    .plus(
      startTimeHours
        + uptrendCheckTimedeltaHours
        + candleTimedeltaHours,
      ChronoUnit.HOURS
    )
}
