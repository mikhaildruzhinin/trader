package com.github.mikhaildruzhinin.trader

import org.postgresql.ds.PGSimpleDataSource
import ru.tinkoff.piapi.core.{InstrumentsService, InvestApi, MarketDataService}

case class Config(tinkoffInvestApi: TinkoffInvestApiConfig,
                  postgres: PostgresConfig,
                  exchange: ExchangeConfig,
                  pctScale: Int,
                  priceScale: Int,
                  uptrendThresholdPct: Int,
                  numUptrendShares: Int,
                  incomeTaxPct: Int)

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

sealed trait InvestApiMode
case object Trade extends InvestApiMode
case object Readonly extends InvestApiMode
case object Sandbox extends InvestApiMode

case class PostgresConfig(host: String,
                          port: Int,
                          db: String,
                          user: String,
                          password: String) {
  val dataSource: PGSimpleDataSource = new PGSimpleDataSource()
  dataSource.setServerNames(Array(host))
  dataSource.setPortNumbers(Array(port))
  dataSource.setDatabaseName(db)
  dataSource.setUser(user)
  dataSource.setPassword(password)
}

case class ExchangeConfig(name: String,
                         startTimeHours: Int,
                         uptrendCheckTimedeltaHours: Int,
                         candleTimedeltaHours: Int)
