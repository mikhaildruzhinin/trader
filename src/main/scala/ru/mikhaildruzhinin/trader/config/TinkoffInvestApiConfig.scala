package ru.mikhaildruzhinin.trader.config

import java.time.temporal.ChronoUnit

sealed trait InvestApiMode

object InvestApiMode {
  case object Trade extends InvestApiMode
  case object Readonly extends InvestApiMode
  case object Sandbox extends InvestApiMode
}

case class TinkoffInvestApiConfig(token: String,
                                  mode: InvestApiMode,
                                  limits: LimitsConfig,
                                  retry: RetryConfig)

case class LimitsConfig(services: ServiceConfig,
                        period: Int,
                        timeUnit: ChronoUnit)

case class ServiceConfig(instruments: Int,
                         marketData: Int,
                         orders: Int,
                         users: Int)

case class RetryConfig(numAttempts: Int,
                       pauseMillis: Long)
