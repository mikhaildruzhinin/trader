package com.github.mikhaildruzhinin.trader

import com.github.kagkarlsson.scheduler.Scheduler
import com.github.kagkarlsson.scheduler.task.helper.{RecurringTask, Tasks}
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import com.github.mikhaildruzhinin.trader.config.{AppConfig, InvestApiMode}
import com.typesafe.scalalogging.Logger
import pureconfig.{ConfigReader, ConfigSource}
import pureconfig.generic.auto.exportReader
import pureconfig.generic.semiauto.deriveEnumerationReader

object TraderScheduler extends App {
  val log: Logger = Logger(getClass.getName.stripSuffix("$"))

  implicit val investApiModeConvert: ConfigReader[InvestApiMode] = deriveEnumerationReader[InvestApiMode]
  implicit val appConfig: AppConfig = ConfigSource.default.loadOrThrow[AppConfig]

  implicit val investApiClient: InvestApiClient.type = InvestApiClient
  implicit val shareWrapper: ShareWrapper.type = ShareWrapper

  val uptrendSharesTask: RecurringTask[Void] = Tasks
    .recurring(
      "uptrend-shares",
      Schedules.cron("*/30 * * * * *") // "0 0 7 * * *"
    )
    .execute((_, _) => log.info("Hello World!"))

  val scheduler: Scheduler = Scheduler
    .create(appConfig.slick.db.properties.dataSource)
    .tableName(appConfig.scheduler.tableName)
    .startTasks(uptrendSharesTask)
    .threads(appConfig.scheduler.numThreads)
    .registerShutdownHook()
    .build()

  scheduler.start()
}
