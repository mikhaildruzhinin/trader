package com.github.mikhaildruzhinin.trader

import com.github.kagkarlsson.scheduler.Scheduler
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import com.github.mikhaildruzhinin.trader.client._
import com.github.mikhaildruzhinin.trader.config.{AppConfig, ConfigReader}
import com.github.mikhaildruzhinin.trader.core.ShareWrapper
import com.github.mikhaildruzhinin.trader.database.SharesTable
import com.typesafe.scalalogging.Logger

import java.time.{Instant, ZoneId}
import scala.concurrent.Await

object Trader extends App {
  val log: Logger = Logger(getClass.getName.stripSuffix("$"))

  implicit val appConfig: AppConfig = ConfigReader.appConfig
  implicit val investApiClient: BaseInvestApiClient = SyncInvestApiClient

    val startUpTask = Tasks.oneTime(
      "start-up"
    ).execute((_,_) => {
      Await.ready(
        SharesTable.createIfNotExists,
        appConfig.slick.await.duration
      )
    })

  val purchaseTask = Tasks.recurring(
    "purchase",
    Schedules.cron(
      "0 0 9 * * *",
      ZoneId.of("UTC")
    )
  ).execute((_,_) => {
    val shares: Seq[ShareWrapper] = ShareWrapper
      .getAvailableShares

    val sharesNum: Option[Int] = Await.result(
      SharesTable.insert(shares.map(_.getShareTuple(1))),
      appConfig.slick.await.duration
    )
    log.info(s"total: ${sharesNum.getOrElse(-1).toString}")

    val uptrendShares: Seq[ShareWrapper] = ShareWrapper
      .getPersistedShares(1)
      .map(_.updateShare)
      .filter(_.uptrendPct >= Some(appConfig.uptrendThresholdPct))
      .sortBy(_.uptrendAbs)
      .reverse
      .take(appConfig.numUptrendShares)

    val uptrendSharesNum: Option[Int] = Await.result(
      SharesTable.insert(uptrendShares.map(_.getShareTuple(2))),
      appConfig.slick.await.duration
    )
    log.info(s"best uptrend: ${uptrendSharesNum.getOrElse(-1).toString}")

    // buy uptrendShares
    val purchasedShares: Seq[ShareWrapper] = ShareWrapper
      .getPersistedShares(2)
      .map(
        s => core.ShareWrapper(
          shareWrapper = s,
          startingPrice = s.startingPrice,
          purchasePrice = s.currentPrice,
          currentPrice = s.currentPrice,
          updateTime = s.updateTime
        )
      )

    val purchasedSharesNum: Option[Int] = Await.result(
      SharesTable.insert(purchasedShares.map(_.getShareTuple(3))),
      appConfig.slick.await.duration
    )
    log.info(s"purchased: ${purchasedSharesNum.getOrElse(-1).toString}")
  })

  val monitorTask = Tasks.recurring(
    "monitor",
    Schedules.cron(
      "0 */2 9-12 * * *",
      ZoneId.of("UTC")
    )
  ).execute((_, _) => {
    val (sharesToSell: List[ShareWrapper], sharesToKeep: Seq[ShareWrapper]) = investApiClient
      .getLastPrices(ShareWrapper.getPersistedShares(3).map(_.figi))
      .zip(ShareWrapper.getPersistedShares(3))
      .map(x => core.ShareWrapper(x._2, x._1))
      .partition(_.roi <= Some(BigDecimal(0)))

    val sellSharesNum: Option[Int] = Await.result(
      SharesTable.insert(sharesToSell.map(_.getShareTuple(4))),
      appConfig.slick.await.duration
    )
    log.info(s"sell: ${sellSharesNum.getOrElse(-1).toString}")

    val keepSharesNum: Option[Int] = Await.result(
      SharesTable.insert(sharesToKeep.map(_.getShareTuple(5))),
      appConfig.slick.await.duration
    )
    log.info(s"keep: ${keepSharesNum.getOrElse(-1).toString}")
  })

  val sellTask = Tasks.recurring(
    "sell",
    Schedules.cron(
      "0 0 13 * * *",
      ZoneId.of("UTC")
    )
  ).execute((_, _) => {
    val soldShares: Seq[ShareWrapper] = ShareWrapper
      .getPersistedShares(5)
      .map(
        s => core.ShareWrapper(
          shareWrapper = s,
          startingPrice = s.startingPrice,
          purchasePrice = s.currentPrice,
          currentPrice = s.currentPrice,
          updateTime = s.updateTime
        )
      )

    val soldSharesNum: Option[Int] = Await.result(
      SharesTable.insert(soldShares.map(_.getShareTuple(4))),
      appConfig.slick.await.duration
    )
    log.info(s"sell: ${soldSharesNum.getOrElse(-1).toString}")
    soldShares.foreach(s => log.info(s.toString))
  })

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
