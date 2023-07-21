package ru.mikhaildruzhinin.trader.config

case class TinkoffInvestApiConfig(token: String,
                                  mode: InvestApiMode,
                                  rateLimitPauseMillis: Long)

sealed trait InvestApiMode

object InvestApiMode {
  case object Trade extends InvestApiMode
  case object Readonly extends InvestApiMode
  case object Sandbox extends InvestApiMode
}
