package ru.mikhaildruzhinin.trader

import com.github.kagkarlsson.scheduler.Scheduler
import com.github.kagkarlsson.scheduler.task.helper.{OneTimeTask, RecurringTask, Tasks}
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import com.typesafe.scalalogging.Logger
import pureconfig.generic.ProductHint
import pureconfig.generic.auto.exportReader
import pureconfig.generic.semiauto.deriveEnumerationReader
import pureconfig.{CamelCase, ConfigFieldMapping, ConfigReader, ConfigSource}
import ru.mikhaildruzhinin.trader.client.base.BaseInvestApiClient
import ru.mikhaildruzhinin.trader.client.impl.ResilientInvestApiClient
import ru.mikhaildruzhinin.trader.config.{AppConfig, InvestApiMode}
import ru.mikhaildruzhinin.trader.core.{Monitor, Purchase}
import ru.mikhaildruzhinin.trader.core.handlers._
import ru.mikhaildruzhinin.trader.core.services.Services
import ru.mikhaildruzhinin.trader.database.{Connection, DatabaseConnection}
import ru.mikhaildruzhinin.trader.database.tables.ShareDAO
import ru.tinkoff.piapi.core.InvestApi

import java.time.ZoneId

trait Components {
  val log: Logger = Logger(getClass.getName)

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
  private val shareDAO: ShareDAO = new ShareDAO(connection.databaseConfig.profile)
  private val services = Services(investApiClient, connection, shareDAO)

  val startUpTask: OneTimeTask[Void] = Tasks
    .oneTime("start-up")
    .execute((_, _) => services.shareService.startUp())

  private val purchaseTask: RecurringTask[Void] = Tasks
    .recurring(
      "purchase",
      Schedules.cron(
        "0 10 9 * * *",
        ZoneId.of("UTC")
      )
    ).execute((_, _) => Purchase(services)
  )

  private val monitorTask: RecurringTask[Void] = Tasks
    .recurring(
      "monitor",
      Schedules.cron(
        "0 12/2 9-12 * * *",
        ZoneId.of("UTC")
      )
    ).execute((_, _) => Monitor(services))

  private val sellTask: RecurringTask[Void] = Tasks
    .recurring(
      "sell",
      Schedules.cron(
        "0 0 13 * * *",
        ZoneId.of("UTC")
      )
    ).execute((_, _) => SellHandler())

  val scheduler: Scheduler = Scheduler
    .create(
      appConfig.slick.db.properties.dataSource,
      startUpTask
    )
    .tableName(appConfig.scheduler.tableName)
    .startTasks(
      purchaseTask,
      monitorTask,
      sellTask
    )
    .threads(appConfig.scheduler.numThreads)
    .registerShutdownHook()
    .build()
}
