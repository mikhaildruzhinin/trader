package ru.mikhaildruzhinin.trader

import com.github.kagkarlsson.scheduler.Scheduler
import com.typesafe.scalalogging.Logger
import ru.mikhaildruzhinin.trader.client.{BaseInvestApiClient, SyncInvestApiClient}
import ru.mikhaildruzhinin.trader.config.{AppConfig, ConfigReader}
import ru.mikhaildruzhinin.trader.core.TaskManager
import ru.mikhaildruzhinin.trader.core.wrappers.ShareWrapper

import java.time.Instant

object Trader extends App {
  val log: Logger = Logger(getClass.getName)

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
