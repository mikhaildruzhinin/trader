package com.github.mikhaildruzhinin.trader

case class Config(tinkoffInvestApi: TinkoffInvestApi,
                  exchange: String,
                  pctScale: Int,
                  priceScale: Int,
                  uptrendThresholdPct: Int,
                  numUptrendShares: Int,
                  incomeTaxPct: Int)

case class TinkoffInvestApi(token: String,
                            rateLimitPauseMillis: Long)
