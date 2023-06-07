package ru.mikhaildruzhinin.trader.core

sealed abstract class TypeCode(val code: Int)

object TypeCode {
  def unapply(typeCode: TypeCode): Option[Int] = Some(typeCode.code)

  final case object Available extends TypeCode(code = 1)
  final case object Uptrend extends TypeCode(code = 2)
  final case object Purchased extends TypeCode(code = 3)
  final case object Sold extends TypeCode(code = 4)
  final case object Kept extends TypeCode(code = 5)
}

