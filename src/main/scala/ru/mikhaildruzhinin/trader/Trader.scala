package ru.mikhaildruzhinin.trader

import com.github.kagkarlsson.scheduler.Scheduler
import com.typesafe.scalalogging.Logger
import ru.mikhaildruzhinin.trader.core.TaskManager

import java.time.Instant

object Trader extends App with Components {
  val log: Logger = Logger(getClass.getName)

  val scheduler: Scheduler = Scheduler
    .create(
      appConfig.slick.db.properties.dataSource,
      TaskManager.getStartUpTask
    )
    .tableName(appConfig.scheduler.tableName)
    .startTasks(
      TaskManager.getPurchaseTask,
      TaskManager.getMonitorTask,
      TaskManager.getSellTask
    )
    .threads(appConfig.scheduler.numThreads)
    .registerShutdownHook()
    .build()

  scheduler.start()

  scheduler
    .schedule(
      TaskManager.getStartUpTask.instance("1"),
      Instant.now.plusSeconds(5)
    )

}
