package ru.mikhaildruzhinin.trader.core

import com.github.kagkarlsson.scheduler.task.helper.{OneTimeTask, RecurringTask, Tasks}
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import ru.mikhaildruzhinin.trader.client.{BaseInvestApiClient, SyncInvestApiClient}
import ru.mikhaildruzhinin.trader.config.{AppConfig, ConfigReader}
import ru.mikhaildruzhinin.trader.core.handlers._

import java.time.ZoneId

object TaskManager {
  implicit val appConfig: AppConfig = ConfigReader.appConfig
  implicit val investApiClient: BaseInvestApiClient = SyncInvestApiClient

  val startUpTask: OneTimeTask[Void] = Tasks.oneTime("start-up")
    .execute(StartUpHandler())

  val purchaseTask: RecurringTask[Void] = Tasks.recurring(
    "purchase",
    Schedules.cron(
      "0 0 9 * * *",
      ZoneId.of("UTC")
    )
  ).execute(PurchaseHandler())

  val monitorTask: RecurringTask[Void] = Tasks.recurring(
    "monitor",
    Schedules.cron(
      "0 */2 9-12 * * *",
      ZoneId.of("UTC")
    )
  ).execute(MonitorHandler())

  val sellTask: RecurringTask[Void] = Tasks.recurring(
    "sell",
    Schedules.cron(
      "0 0 13 * * *",
      ZoneId.of("UTC")
    )
  ).execute(SellHandler())
}
