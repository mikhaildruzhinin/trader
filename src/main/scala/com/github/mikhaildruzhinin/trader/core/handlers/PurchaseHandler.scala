package com.github.mikhaildruzhinin.trader.core.handlers

import com.github.kagkarlsson.scheduler.task.{ExecutionContext, TaskInstance, VoidExecutionHandler}
import com.github.mikhaildruzhinin.trader.client.BaseInvestApiClient
import com.github.mikhaildruzhinin.trader.config.AppConfig
import com.github.mikhaildruzhinin.trader.core.ShareWrapper
import com.github.mikhaildruzhinin.trader.database.SharesTable
import com.typesafe.scalalogging.Logger

import scala.concurrent.Await

class PurchaseHandler[T](implicit appConfig: AppConfig,
                         investApiClient: BaseInvestApiClient) extends VoidExecutionHandler[T] {
  val log: Logger = Logger(getClass.getName.stripSuffix("$"))

  override def execute(taskInstance: TaskInstance[T],
                       executionContext: ExecutionContext): Unit = {

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
        s => ShareWrapper(
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
  }
}

object PurchaseHandler {
  def apply()(implicit appConfig: AppConfig,
              investApiClient: BaseInvestApiClient) = new PurchaseHandler[Void]()
}
