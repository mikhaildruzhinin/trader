package com.github.mikhaildruzhinin.trader

import org.postgresql.ds.PGSimpleDataSource

case class Config(tinkoffInvestApi: TinkoffInvestApiConfig,
                  postgres: PostgresConfig,
                  exchange: ExchangeConfig,
                  pctScale: Int,
                  priceScale: Int,
                  uptrendThresholdPct: Int,
                  numUptrendShares: Int,
                  incomeTaxPct: Int) {

  val dataSource: PGSimpleDataSource = new PGSimpleDataSource()
  dataSource.setServerNames(Array(postgres.host))
  dataSource.setPortNumbers(Array(postgres.port))
  dataSource.setDatabaseName(postgres.db)
  dataSource.setUser(postgres.user)
  dataSource.setPassword(postgres.password)
}

case class TinkoffInvestApiConfig(token: String,
                                  rateLimitPauseMillis: Long)

  case class PostgresConfig(host: String,
                            port: Int,
                            db: String,
                            user: String,
                            password: String)

case class ExchangeConfig(name: String,
                         startTimeHours: Int,
                         uptrendCheckTimedeltaHours: Int,
                         candleTimedeltaHours: Int)
