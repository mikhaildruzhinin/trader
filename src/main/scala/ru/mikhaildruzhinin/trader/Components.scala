package ru.mikhaildruzhinin.trader

import ru.mikhaildruzhinin.trader.client.{BaseInvestApiClient, FaultTolerance, ResilientInvestApiClient}
import ru.mikhaildruzhinin.trader.config.{AppConfig, AppConfigReader, InvestApiMode}
import ru.mikhaildruzhinin.trader.database.connection.{Connection, DatabaseConnection}
import ru.tinkoff.piapi.core.InvestApi

trait Components {
  implicit lazy val appConfig: AppConfig = AppConfigReader.appConfig
  lazy val investApi: InvestApi = appConfig.tinkoffInvestApi.mode match {
    case InvestApiMode.Trade => InvestApi.create(appConfig.tinkoffInvestApi.token)
    case InvestApiMode.Readonly => InvestApi.createReadonly(appConfig.tinkoffInvestApi.token)
    case InvestApiMode.Sandbox => InvestApi.createSandbox(appConfig.tinkoffInvestApi.token)
  }
  val faultTolerance: FaultTolerance = new FaultTolerance()

  implicit lazy val investApiClient: BaseInvestApiClient = new ResilientInvestApiClient(
    investApi,
    faultTolerance
  )
  implicit lazy val connection: Connection = DatabaseConnection
}
