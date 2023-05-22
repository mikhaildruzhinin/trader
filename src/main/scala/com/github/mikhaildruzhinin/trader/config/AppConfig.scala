package com.github.mikhaildruzhinin.trader.config

case class AppConfig(tinkoffInvestApi: TinkoffInvestApiConfig,
                     postgres: PostgresConfig,
                     exchange: ExchangeConfig,
                     scheduler: SchedulerConfig,
                     pctScale: Int,
                     priceScale: Int,
                     uptrendThresholdPct: Int,
                     numUptrendShares: Int,
                     incomeTaxPct: Int)
