package com.github.mikhaildruzhinin.trader

import com.github.kagkarlsson.scheduler.Scheduler
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import com.github.mikhaildruzhinin.trader.client._
import com.github.mikhaildruzhinin.trader.config.{AppConfig, ConfigReader}
import com.github.mikhaildruzhinin.trader.core.handlers._
import com.typesafe.scalalogging.Logger

import java.time.{Instant, ZoneId}

object Trader extends App {
  val log: Logger = Logger(getClass.getName.stripSuffix("$"))

  implicit val appConfig: AppConfig = ConfigReader.appConfig
  implicit val investApiClient: BaseInvestApiClient = SyncInvestApiClient

    val startUpTask = Tasks.oneTime("start-up")
      .execute(StartUpHandler())

  val purchaseTask = Tasks.recurring(
    "purchase",
    Schedules.cron(
      "0 0 9 * * *",
      ZoneId.of("UTC")
    )
  ).execute(PurchaseHandler())

  val monitorTask = Tasks.recurring(
    "monitor",
    Schedules.cron(
      "0 */2 9-12 * * *",
      ZoneId.of("UTC")
    )
  ).execute(MonitorHandler())

  val sellTask = Tasks.recurring(
    "sell",
    Schedules.cron(
      "0 0 13 * * *",
      ZoneId.of("UTC")
    )
  ).execute(SellHandler())

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

  scheduler.start()

  scheduler
    .schedule(
      startUpTask.instance("1"),
      Instant.now.plusSeconds(5)
    )
}
