package com.github.mikhaildruzhinin.trader.core.handlers

import com.github.kagkarlsson.scheduler.task.{ExecutionContext, TaskInstance, VoidExecutionHandler}
import com.github.mikhaildruzhinin.trader.client.BaseInvestApiClient
import com.github.mikhaildruzhinin.trader.config.AppConfig
import com.github.mikhaildruzhinin.trader.core.ShareWrapper
import com.github.mikhaildruzhinin.trader.database.SharesTable
import com.typesafe.scalalogging.Logger

import scala.concurrent.Await

class MonitorHandler[T](implicit appConfig: AppConfig,
                        investApiClient: BaseInvestApiClient) extends VoidExecutionHandler[T] {
  val log: Logger = Logger(getClass.getName.stripSuffix("$"))

  override def execute(taskInstance: TaskInstance[T],
                       executionContext: ExecutionContext): Unit = {
    val (sharesToSell: List[ShareWrapper], sharesToKeep: Seq[ShareWrapper]) = investApiClient
      .getLastPrices(ShareWrapper.getPersistedShares(3).map(_.figi))
      .zip(ShareWrapper.getPersistedShares(3))
      .map(x => ShareWrapper(x._2, x._1))
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
  }
}

object MonitorHandler {
  def apply()(implicit appConfig: AppConfig,
              investApiClient: BaseInvestApiClient) = new MonitorHandler[Void]()
}
