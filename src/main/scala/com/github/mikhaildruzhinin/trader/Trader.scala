package com.github.mikhaildruzhinin.trader

import com.github.kagkarlsson.scheduler.Scheduler
import com.github.mikhaildruzhinin.trader.client._
import com.github.mikhaildruzhinin.trader.config.{AppConfig, ConfigReader}
import com.github.mikhaildruzhinin.trader.core.TaskManager
import com.typesafe.scalalogging.Logger

import java.time.Instant

object Trader extends App {
  val log: Logger = Logger(getClass.getName.stripSuffix("$"))

  implicit val appConfig: AppConfig = ConfigReader.appConfig
  implicit val investApiClient: BaseInvestApiClient = SyncInvestApiClient

  val scheduler: Scheduler = Scheduler
    .create(
      appConfig.slick.db.properties.dataSource,
      TaskManager.startUpTask
    )
    .tableName(appConfig.scheduler.tableName)
    .startTasks(
      TaskManager.purchaseTask,
      TaskManager.monitorTask,
      TaskManager.sellTask
    )
    .threads(appConfig.scheduler.numThreads)
    .registerShutdownHook()
    .build()

  scheduler.start()

  scheduler
    .schedule(
      TaskManager.startUpTask.instance("1"),
      Instant.now.plusSeconds(5)
    )
}
