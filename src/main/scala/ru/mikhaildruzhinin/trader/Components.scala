package ru.mikhaildruzhinin.trader

import ru.mikhaildruzhinin.trader.client.{BaseInvestApiClient, InvestApiClient}
import ru.mikhaildruzhinin.trader.config.{AppConfig, AppConfigReader, InvestApiMode}
import ru.mikhaildruzhinin.trader.database.connection.{Connection, DatabaseConnection}
import ru.tinkoff.piapi.core.InvestApi

trait Components {
  implicit lazy val appConfig: AppConfig = AppConfigReader.appConfig
  implicit lazy val investApi: InvestApi = appConfig.tinkoffInvestApi.mode match {
    case InvestApiMode.Trade => InvestApi.create(appConfig.tinkoffInvestApi.token)
    case InvestApiMode.Readonly => InvestApi.createReadonly(appConfig.tinkoffInvestApi.token)
    case InvestApiMode.Sandbox => InvestApi.createSandbox(appConfig.tinkoffInvestApi.token)
  }
  implicit lazy val investApiClient: BaseInvestApiClient = InvestApiClient()
  implicit lazy val connection: Connection = DatabaseConnection
}
