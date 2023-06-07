package ru.mikhaildruzhinin.trader.config

import ru.mikhaildruzhinin.trader.config.exchange.ExchangeConfig
import ru.mikhaildruzhinin.trader.config.scheduler.SchedulerConfig
import ru.mikhaildruzhinin.trader.config.slick.SlickConfig
import ru.mikhaildruzhinin.trader.config.tinkoff.TinkoffInvestApiConfig

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
