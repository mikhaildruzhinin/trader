package com.github.mikhaildruzhinin.trader.config

case class AppConfig(tinkoffInvestApi: TinkoffInvestApiConfig,
                     slick: SlickConfig,
                     exchange: ExchangeConfig,
                     scheduler: SchedulerConfig,
                     pctScale: Int,
                     priceScale: Int,
                     uptrendThresholdPct: Int,
                     numUptrendShares: Int,
                     incomeTaxPct: Int,
                     testFlg: Boolean)
