package com.github.mikhaildruzhinin.trader

case class Config(tinkoffInvestApiToken: String,
                  exchange: String,
                  pctScale: Int,
                  priceScale: Int,
                  uptrendThresholdPct: Int,
                  numUptrendShares: Int,
                  incomeTaxPct: Int)
