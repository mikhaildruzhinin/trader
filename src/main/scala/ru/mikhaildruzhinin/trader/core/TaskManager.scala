package ru.mikhaildruzhinin.trader.core

import com.github.kagkarlsson.scheduler.task.helper.{OneTimeTask, RecurringTask, Tasks}
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import ru.mikhaildruzhinin.trader.client.{BaseInvestApiClient, SyncInvestApiClient}
import ru.mikhaildruzhinin.trader.config.{AppConfig, ConfigReader}
import ru.mikhaildruzhinin.trader.core.handlers._
import ru.mikhaildruzhinin.trader.database.connection.{Connection, DatabaseConnection}

import java.time.ZoneId

object TaskManager {
  implicit val appConfig: AppConfig = ConfigReader.appConfig
  implicit val connection: Connection = DatabaseConnection
  implicit val investApiClient: BaseInvestApiClient = SyncInvestApiClient

  val startUpTask: OneTimeTask[Void] = Tasks.oneTime("start-up")
    .execute((_, _) => StartUpHandler())

  val purchaseTask: RecurringTask[Void] = Tasks.recurring(
    "purchase",
    Schedules.cron(
      "0 10 9 * * *",
      ZoneId.of("UTC")
    )
  ).execute((_, _) => PurchaseHandler())

  val monitorTask: RecurringTask[Void] = Tasks.recurring(
    "monitor",
    Schedules.cron(
      "0 12/2 9-12 * * *",
      ZoneId.of("UTC")
    )
  ).execute((_, _) => MonitorHandler())

  val sellTask: RecurringTask[Void] = Tasks.recurring(
    "sell",
    Schedules.cron(
      "0 0 13 * * *",
      ZoneId.of("UTC")
    )
  ).execute((_, _) => SellHandler())
}
