package com.github.mikhaildruzhinin.trader.config

import com.github.mikhaildruzhinin.trader.config.exchange.ExchangeConfig
import com.github.mikhaildruzhinin.trader.config.scheduler.SchedulerConfig
import com.github.mikhaildruzhinin.trader.config.slick.SlickConfig
import com.github.mikhaildruzhinin.trader.config.tinkoff.TinkoffInvestApiConfig

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
