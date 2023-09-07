package ru.mikhaildruzhinin.trader

import com.github.kagkarlsson.scheduler.Scheduler
import org.flywaydb.core.Flyway
import pureconfig.generic.ProductHint
import pureconfig.generic.auto.exportReader
import pureconfig.generic.semiauto.deriveEnumerationReader
import pureconfig.{CamelCase, ConfigFieldMapping, ConfigReader, ConfigSource}
import ru.mikhaildruzhinin.trader.client.base.BaseInvestApiClient
import ru.mikhaildruzhinin.trader.client.impl.ResilientInvestApiClient
import ru.mikhaildruzhinin.trader.config.{AppConfig, InvestApiMode}
import ru.mikhaildruzhinin.trader.core.services.base._
import ru.mikhaildruzhinin.trader.core.services.impl._
import ru.mikhaildruzhinin.trader.database.tables.base.BaseShareDAO
import ru.mikhaildruzhinin.trader.database.tables.impl.ShareDAO
import ru.mikhaildruzhinin.trader.database.{Connection, ConnectionImpl}
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

  lazy val investApiClient: BaseInvestApiClient = wire[ResilientInvestApiClient]
  lazy val connection: Connection = ConnectionImpl
  lazy val shareDAO: BaseShareDAO = wire[ShareDAO]
  lazy val shareService: BaseShareService = wire[ShareService]
  lazy val historicCandleService: BaseHistoricCandleService = wire[HistoricCandleService]
  lazy val priceService: BasePriceService = wire[PriceService]
  lazy val accountService: BaseAccountService = wire[AccountService]
  lazy val scheduler: Scheduler = SchedulerFactory(shareService)

  Flyway.configure()
    .dataSource(appConfig.slick.db.properties.dataSource)
    .schemas("trader")
    .load()
    .migrate()
}
