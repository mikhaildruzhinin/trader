package ru.mikhaildruzhinin.trader.config.tinkoff

import ru.tinkoff.piapi.core.{InstrumentsService, InvestApi, MarketDataService}

case class TinkoffInvestApiConfig(token: String,
                                  mode: InvestApiMode,
                                  rateLimitPauseMillis: Long) {

  lazy val api: InvestApi = mode match {
    case InvestApiMode.Trade => InvestApi.create(token)
    case InvestApiMode.Readonly => InvestApi.createReadonly(token)
    case InvestApiMode.Sandbox => InvestApi.createSandbox(token)
  }
  lazy val instrumentService: InstrumentsService = api.getInstrumentsService
  lazy val marketDataService: MarketDataService = api.getMarketDataService
}
