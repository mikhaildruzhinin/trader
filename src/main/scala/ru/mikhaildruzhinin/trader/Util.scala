package ru.mikhaildruzhinin.trader

import java.sql.Timestamp
import java.time.LocalDate
import java.time.temporal.ChronoUnit

sealed abstract class TypeCode(val code: Int)

object TypeCode {
  final case object Available extends TypeCode(code = 1)
  final case object Uptrend extends TypeCode(code = 2)
  final case object Purchased extends TypeCode(code = 3)
  final case object Sold extends TypeCode(code = 4)
}

case class DayInterval(start: Timestamp, end: Timestamp)

object DayInterval {
  def apply(): DayInterval = {
    val start: Timestamp = Timestamp.valueOf(LocalDate.now.atStartOfDay)

    val end: Timestamp = Timestamp.valueOf(
      LocalDate.now.plus(1, ChronoUnit.DAYS).atStartOfDay
    )

    new DayInterval(start, end)
  }
}

