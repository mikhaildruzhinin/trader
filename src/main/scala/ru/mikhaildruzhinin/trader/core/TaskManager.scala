package ru.mikhaildruzhinin.trader.core

import com.github.kagkarlsson.scheduler.task.helper.{OneTimeTask, RecurringTask, Tasks}
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import ru.mikhaildruzhinin.trader.client.BaseInvestApiClient
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.mikhaildruzhinin.trader.core.handlers._
import ru.mikhaildruzhinin.trader.database.connection.Connection

import java.time.ZoneId

object TaskManager {
  def getStartUpTask(implicit appConfig: AppConfig,
                     connection: Connection,
                     investApiClient: BaseInvestApiClient): OneTimeTask[Void] = Tasks
    .oneTime("start-up")
    .execute((_, _) => StartUpHandler())

  def getPurchaseTask(implicit appConfig: AppConfig,
                      connection: Connection,
                      investApiClient: BaseInvestApiClient): RecurringTask[Void] = Tasks
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

  def getMonitorTask(implicit appConfig: AppConfig,
                     connection: Connection,
                     investApiClient: BaseInvestApiClient): RecurringTask[Void] = Tasks
    .recurring(
      "monitor",
      Schedules.cron(
        "0 12/2 9-12 * * *",
        ZoneId.of("UTC")
      )
    ).execute((_, _) => MonitorHandler())

  def getSellTask(implicit appConfig: AppConfig,
                  connection: Connection,
                  investApiClient: BaseInvestApiClient): RecurringTask[Void] = Tasks
    .recurring(
      "sell",
      Schedules.cron(
        "0 0 13 * * *",
        ZoneId.of("UTC")
      )
    ).execute((_, _) => SellHandler())
}
