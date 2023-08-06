package ru.mikhaildruzhinin.trader

import com.github.kagkarlsson.scheduler.Scheduler

import java.time.Instant

object Trader extends App with Components {
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
