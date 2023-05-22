package com.github.mikhaildruzhinin.trader.config

case class Config(tinkoffInvestApi: TinkoffInvestApiConfig,
                  postgres: PostgresConfig,
                  exchange: ExchangeConfig,
                  pctScale: Int,
                  priceScale: Int,
                  uptrendThresholdPct: Int,
                  numUptrendShares: Int,
                  incomeTaxPct: Int)
