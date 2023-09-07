package ru.mikhaildruzhinin.trader.config

sealed abstract class TypeCode(val code: Int)

object TypeCode {
  final case object Available extends TypeCode(code = 1)
  final case object Uptrend extends TypeCode(code = 2)
  final case object Purchased extends TypeCode(code = 3)
  final case object Sold extends TypeCode(code = 4)
}

