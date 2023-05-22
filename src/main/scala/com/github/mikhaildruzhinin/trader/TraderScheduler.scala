package com.github.mikhaildruzhinin.trader

import com.github.kagkarlsson.scheduler.Scheduler
import com.github.kagkarlsson.scheduler.task.helper.{RecurringTask, Tasks}
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import com.typesafe.scalalogging.Logger
import pureconfig.ConfigSource
import pureconfig.generic.auto.exportReader

object TraderScheduler extends App {
  val log: Logger = Logger(getClass.getName.stripSuffix("$"))

  implicit val config: Config = ConfigSource.default.loadOrThrow[Config]

  implicit val investApiClient: InvestApiClient.type = InvestApiClient
  implicit val shareWrapper: ShareWrapper.type = ShareWrapper

  val uptrendSharesTask: RecurringTask[Void] = Tasks
    .recurring(
      "uptrend-shares",
      Schedules.cron("*/30 * * * * *") // "0 0 7 * * *"
    )
    .execute((_, _) => log.info("Hello World!"))

  val scheduler: Scheduler = Scheduler
    .create(config.postgres.dataSource)
    .startTasks(uptrendSharesTask)
    .threads(5)
    .registerShutdownHook()
    .build()

  scheduler.start()
}
