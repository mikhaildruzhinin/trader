package com.github.mikhaildruzhinin.trader.config

import ru.tinkoff.piapi.core.{InstrumentsService, InvestApi, MarketDataService}

sealed trait InvestApiMode
case object Trade extends InvestApiMode
case object Readonly extends InvestApiMode
case object Sandbox extends InvestApiMode

case class TinkoffInvestApiConfig(token: String,
                                  mode: InvestApiMode,
                                  rateLimitPauseMillis: Long) {

  val api: InvestApi = mode match {
    case Trade => InvestApi.create(token)
    case Readonly => InvestApi.createReadonly(token)
    case Sandbox => InvestApi.createSandbox(token)
  }
  lazy val instrumentService: InstrumentsService = api.getInstrumentsService
  lazy val marketDataService: MarketDataService = api.getMarketDataService
}
