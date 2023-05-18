package com.github.mikhaildruzhinin.trader

import com.github.kagkarlsson.scheduler.Scheduler
import com.github.kagkarlsson.scheduler.task.helper.{RecurringTask, Tasks}
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import com.typesafe.scalalogging.Logger
import org.postgresql.ds.PGSimpleDataSource
import pureconfig.ConfigSource
import pureconfig.generic.auto.exportReader

object TraderScheduler extends App {
  val log: Logger = Logger(getClass.getName.stripSuffix("$"))

  implicit lazy val config: Config = ConfigSource.default.loadOrThrow[Config]

  val dataSource: PGSimpleDataSource = new PGSimpleDataSource()
  dataSource.setServerNames(Array(config.postgres.host))
  dataSource.setPortNumbers(Array(config.postgres.port))
  dataSource.setDatabaseName(config.postgres.db)
  dataSource.setUser(config.postgres.user)
  dataSource.setPassword(config.postgres.password)

  val task: RecurringTask[Void] = Tasks
    .recurring(
      "task",
      Schedules.cron("*/10 * * * * ?")
    )
    .execute((_, _) => log.info("Executed!"))

  val scheduler: Scheduler = Scheduler
    .create(dataSource)
    .startTasks(task)
    .threads(5)
    .registerShutdownHook()
    .build()

  scheduler.start()
}
