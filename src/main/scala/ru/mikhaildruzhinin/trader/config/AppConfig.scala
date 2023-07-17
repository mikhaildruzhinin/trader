package ru.mikhaildruzhinin.trader.config

case class AppConfig(tinkoffInvestApi: TinkoffInvestApiConfig,
                     slick: SlickConfig,
                     exchange: ExchangeConfig,
                     scheduler: SchedulerConfig,
                     shares: SharesConfig,
                     testFlg: Boolean)

case class SchedulerConfig(tableName: String,
                           numThreads: Int)

case class SharesConfig(pctScale: Int,
                        priceScale: Int,
                        uptrendThresholdPct: Int,
                        numUptrendShares: Int,
                        incomeTaxPct: Int)
