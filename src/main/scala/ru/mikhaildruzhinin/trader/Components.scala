package ru.mikhaildruzhinin.trader

import com.github.kagkarlsson.scheduler.Scheduler
import pureconfig.generic.ProductHint
import pureconfig.generic.auto.exportReader
import pureconfig.generic.semiauto.deriveEnumerationReader
import pureconfig.{CamelCase, ConfigFieldMapping, ConfigReader, ConfigSource}
import ru.mikhaildruzhinin.trader.client.base.BaseInvestApiClient
import ru.mikhaildruzhinin.trader.client.impl.ResilientInvestApiClient
import ru.mikhaildruzhinin.trader.config.{AppConfig, InvestApiMode}
import ru.mikhaildruzhinin.trader.core.services.Services
import ru.mikhaildruzhinin.trader.database.tables.ShareDAO
import ru.mikhaildruzhinin.trader.database.{Connection, DatabaseConnection}
import ru.tinkoff.piapi.core.InvestApi

trait Components {
  private implicit def hint[A]: ProductHint[A] = ProductHint[A](ConfigFieldMapping(CamelCase, CamelCase))
  private implicit lazy val investApiModeConvert: ConfigReader[InvestApiMode] = deriveEnumerationReader[InvestApiMode]
  implicit lazy val appConfig: AppConfig = ConfigSource.default.loadOrThrow[AppConfig]

  lazy val investApi: InvestApi = appConfig.tinkoffInvestApi.mode match {
    case InvestApiMode.Trade => InvestApi.create(appConfig.tinkoffInvestApi.token)
    case InvestApiMode.Readonly => InvestApi.createReadonly(appConfig.tinkoffInvestApi.token)
    case InvestApiMode.Sandbox => InvestApi.createSandbox(appConfig.tinkoffInvestApi.token)
  }

  implicit lazy val investApiClient: BaseInvestApiClient = ResilientInvestApiClient(investApi)
  implicit lazy val connection: Connection = DatabaseConnection
  private lazy val shareDAO: ShareDAO = new ShareDAO(connection.databaseConfig.profile)
  private lazy val services = Services(investApiClient, connection, shareDAO)
  lazy val scheduler: Scheduler = SchedulerFactory(services)

  // TODO: move startUp to flyway migrations
  services.shareService.startUp()
}
