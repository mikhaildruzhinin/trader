package com.github.mikhaildruzhinin.trader.config.tinkoff

sealed trait InvestApiMode

object InvestApiMode {
  case object Trade extends InvestApiMode
  case object Readonly extends InvestApiMode
  case object Sandbox extends InvestApiMode
}
