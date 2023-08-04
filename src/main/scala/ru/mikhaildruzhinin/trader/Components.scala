package ru.mikhaildruzhinin.trader

import com.github.kagkarlsson.scheduler.task.helper.{OneTimeTask, RecurringTask, Tasks}
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import pureconfig.generic.ProductHint
import pureconfig.generic.auto.exportReader
import pureconfig.generic.semiauto.deriveEnumerationReader
import pureconfig._
import ru.mikhaildruzhinin.trader.client.{BaseInvestApiClient, FaultTolerance, ResilientInvestApiClient}
import ru.mikhaildruzhinin.trader.config.{AppConfig, InvestApiMode}
import ru.mikhaildruzhinin.trader.core.handlers._
import ru.mikhaildruzhinin.trader.database.connection.{Connection, DatabaseConnection}
import ru.tinkoff.piapi.core.InvestApi

import java.time.ZoneId

trait Components {
  private implicit def hint[A]: ProductHint[A] = ProductHint[A](ConfigFieldMapping(CamelCase, CamelCase))
  private implicit lazy val investApiModeConvert: ConfigReader[InvestApiMode] = deriveEnumerationReader[InvestApiMode]
  implicit lazy val appConfig: AppConfig = ConfigSource.default.loadOrThrow[AppConfig]

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

  val startUpTask: OneTimeTask[Void] = Tasks
    .oneTime("start-up")
    .execute((_, _) => StartUpHandler())

  val purchaseTask: RecurringTask[Void] = Tasks
    .recurring(
      "purchase",
      Schedules.cron(
        "0 10 9 * * *",
        ZoneId.of("UTC")
      )
    ).execute((_, _) => {
    AvailabilityHandler()
    UptrendHandler()
    PurchaseHandler()
  })

  val monitorTask: RecurringTask[Void] = Tasks
    .recurring(
      "monitor",
      Schedules.cron(
        "0 12/2 9-12 * * *",
        ZoneId.of("UTC")
      )
    ).execute((_, _) => MonitorHandler())

  val sellTask: RecurringTask[Void] = Tasks
    .recurring(
      "sell",
      Schedules.cron(
        "0 0 13 * * *",
        ZoneId.of("UTC")
      )
    ).execute((_, _) => SellHandler())
}
