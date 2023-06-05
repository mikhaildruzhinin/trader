package com.github.mikhaildruzhinin.trader.core.handlers

import com.github.kagkarlsson.scheduler.task.{ExecutionContext, TaskInstance, VoidExecutionHandler}
import com.github.mikhaildruzhinin.trader.client.BaseInvestApiClient
import com.github.mikhaildruzhinin.trader.config.AppConfig
import com.github.mikhaildruzhinin.trader.core.ShareWrapper
import com.github.mikhaildruzhinin.trader.database.SharesTable
import com.typesafe.scalalogging.Logger

import scala.concurrent.Await

class SellHandler[T](implicit appConfig: AppConfig) extends VoidExecutionHandler[T] {
  val log: Logger = Logger(getClass.getName.stripSuffix("$"))

  override def execute(taskInstance: TaskInstance[T],
                       executionContext: ExecutionContext): Unit = {

    val soldShares: Seq[ShareWrapper] = ShareWrapper
      .getPersistedShares(5)
      .map(
        s => ShareWrapper(
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
  }
}

object SellHandler {
  def apply()(implicit appConfig: AppConfig) = new SellHandler[Void]()
}
