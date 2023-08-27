package ru.mikhaildruzhinin.trader

import com.github.kagkarlsson.scheduler.Scheduler
import com.github.kagkarlsson.scheduler.task.helper.{RecurringTask, Tasks}
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.mikhaildruzhinin.trader.core.services.Services
import ru.mikhaildruzhinin.trader.core.{Monitor, Purchase, Sell}

import java.time.ZoneId

object SchedulerFactory {
  def apply(services: Services)
           (implicit appConfig: AppConfig): Scheduler = {

    val purchaseTask: RecurringTask[Void] = Tasks
      .recurring(
        "purchase",
        Schedules.cron(
          "0 10 9 * * *",
          ZoneId.of("UTC")
        )
      ).execute((_, _) => Purchase(services)
    )

    val monitorTask: RecurringTask[Void] = Tasks
      .recurring(
        "monitor",
        Schedules.cron(
          "0 12/2 9-12 * * *",
          ZoneId.of("UTC")
        )
      ).execute((_, _) => Monitor(services))

    val sellTask: RecurringTask[Void] = Tasks
      .recurring(
        "sell",
        Schedules.cron(
          "0 0 13 * * *",
          ZoneId.of("UTC")
        )
      ).execute((_, _) => Sell(services))

    Scheduler
      .create(appConfig.slick.db.properties.dataSource)
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
}
