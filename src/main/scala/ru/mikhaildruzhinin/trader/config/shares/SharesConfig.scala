package ru.mikhaildruzhinin.trader.config.shares

case class SharesConfig(pctScale: Int,
                        priceScale: Int,
                        uptrendThresholdPct: Int,
                        numUptrendShares: Int,
                        incomeTaxPct: Int)
