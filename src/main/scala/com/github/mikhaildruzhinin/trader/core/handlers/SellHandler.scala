package com.github.mikhaildruzhinin.trader.core.handlers

import com.github.kagkarlsson.scheduler.task.{ExecutionContext, TaskInstance, VoidExecutionHandler}
import com.github.mikhaildruzhinin.trader.config.AppConfig
import com.github.mikhaildruzhinin.trader.core.{ShareWrapper, TypeCode}
import com.github.mikhaildruzhinin.trader.database.SharesTable
import com.typesafe.scalalogging.Logger

import scala.concurrent.Await

class SellHandler[T](implicit appConfig: AppConfig) extends VoidExecutionHandler[T] {
  val log: Logger = Logger(getClass.getName.stripSuffix("$"))

  override def execute(taskInstance: TaskInstance[T],
                       executionContext: ExecutionContext): Unit = {

    val sharesToSell: Seq[ShareWrapper] = ShareWrapper
      .getPersistedShares(TypeCode.Kept)
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
      SharesTable.insert(sharesToSell.map(_.getShareTuple(TypeCode.Sold))),
      appConfig.slick.await.duration
    )
    log.info(s"sell: ${soldSharesNum.getOrElse(-1).toString}")
    sharesToSell.foreach(s => log.info(s.toString))
  }
}

object SellHandler {
  def apply()(implicit appConfig: AppConfig) = new SellHandler[Void]()
}
