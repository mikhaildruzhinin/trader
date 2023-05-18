package com.github.mikhaildruzhinin.trader

case class Config(tinkoffInvestApi: TinkoffInvestApiConfig,
                  postgres: PostgresConfig,
                  exchange: ExchangeConfig,
                  pctScale: Int,
                  priceScale: Int,
                  uptrendThresholdPct: Int,
                  numUptrendShares: Int,
                  incomeTaxPct: Int)

case class TinkoffInvestApiConfig(token: String,
                                  rateLimitPauseMillis: Long)

  case class PostgresConfig(host: String,
                            port: Int,
                            db: String,
                            user: String,
                            password: String)

case class ExchangeConfig(name: String,
                         startTimeHours: Int,
                         uptrendCheckTimedeltaHours: Int,
                         candleTimedeltaHours: Int)
