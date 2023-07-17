package ru.mikhaildruzhinin.trader.config

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

sealed trait InvestApiMode

object InvestApiMode {
  case object Trade extends InvestApiMode
  case object Readonly extends InvestApiMode
  case object Sandbox extends InvestApiMode
}
