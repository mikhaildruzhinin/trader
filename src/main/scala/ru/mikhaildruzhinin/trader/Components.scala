package ru.mikhaildruzhinin.trader

import com.github.kagkarlsson.scheduler.Scheduler
import org.flywaydb.core.Flyway
import pureconfig.generic.ProductHint
import pureconfig.generic.auto.exportReader
import pureconfig.generic.semiauto.deriveEnumerationReader
import pureconfig.{CamelCase, ConfigFieldMapping, ConfigReader, ConfigSource}
import ru.mikhaildruzhinin.trader.client.InvestApiClient
import ru.mikhaildruzhinin.trader.client.impl.ResilientInvestApiClientImpl
import ru.mikhaildruzhinin.trader.config.{AppConfig, InvestApiMode}
import ru.mikhaildruzhinin.trader.database.tables.ShareDAO
import ru.mikhaildruzhinin.trader.database.tables.impl.ShareDAOImpl
import ru.mikhaildruzhinin.trader.database.{Connection, ConnectionImpl}
import ru.mikhaildruzhinin.trader.services._
import ru.mikhaildruzhinin.trader.services.impl._
import ru.tinkoff.piapi.core.InvestApi

trait Components {
  import com.softwaremill.macwire.wire

  private implicit def hint[A]: ProductHint[A] = ProductHint[A](ConfigFieldMapping(CamelCase, CamelCase))
  private implicit lazy val investApiModeConvert: ConfigReader[InvestApiMode] = deriveEnumerationReader[InvestApiMode]
  implicit lazy val appConfig: AppConfig = ConfigSource.default.loadOrThrow[AppConfig]

  lazy val investApi: InvestApi = appConfig.tinkoffInvestApi.mode match {
    case InvestApiMode.Trade => InvestApi.create(appConfig.tinkoffInvestApi.token)
    case InvestApiMode.Readonly => InvestApi.createReadonly(appConfig.tinkoffInvestApi.token)
    case InvestApiMode.Sandbox => InvestApi.createSandbox(appConfig.tinkoffInvestApi.token)
  }

  lazy val investApiClient: InvestApiClient = wire[ResilientInvestApiClientImpl]
  lazy val connection: Connection = ConnectionImpl
  lazy val shareDAO: ShareDAO = wire[ShareDAOImpl]
  lazy val shareService: ShareService = wire[ShareServiceImpl]
  lazy val candleService: CandleService = wire[CandleServiceImpl]
  lazy val priceService: PriceService = wire[PriceServiceImpl]
  lazy val accountService: AccountService = wire[AccountServiceImpl]
  lazy val scheduler: Scheduler = SchedulerFactory(shareService)

  Flyway.configure()
    .dataSource(appConfig.slick.db.properties.dataSource)
    .schemas("trader")
    .load()
    .migrate()
}
