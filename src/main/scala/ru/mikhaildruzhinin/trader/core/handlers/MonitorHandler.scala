package ru.mikhaildruzhinin.trader.core.handlers

import com.github.kagkarlsson.scheduler.task.{ExecutionContext, TaskInstance, VoidExecutionHandler}
import com.typesafe.scalalogging.Logger
import ru.mikhaildruzhinin.trader.client.BaseInvestApiClient
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.mikhaildruzhinin.trader.core.{ShareWrapper, TypeCode}
import ru.mikhaildruzhinin.trader.database.SharesTable

import scala.concurrent.Await

class MonitorHandler[T](implicit appConfig: AppConfig,
                        investApiClient: BaseInvestApiClient) extends VoidExecutionHandler[T] {
  val log: Logger = Logger(getClass.getName.stripSuffix("$"))

  override def execute(taskInstance: TaskInstance[T],
                       executionContext: ExecutionContext): Unit = {

    val (sharesToSell: List[ShareWrapper], sharesToKeep: Seq[ShareWrapper]) = investApiClient
      .getLastPrices(ShareWrapper.getPersistedShares(TypeCode.Purchased).map(_.figi))
      .zip(ShareWrapper.getPersistedShares(TypeCode.Purchased))
      .map(x => ShareWrapper(x._2, x._1))
      .partition(_.roi <= Some(BigDecimal(0)))

    val sellSharesNum: Option[Int] = Await.result(
      SharesTable.insert(sharesToSell.map(_.getShareTuple(TypeCode.Sold))),
      appConfig.slick.await.duration
    )
    log.info(s"sell: ${sellSharesNum.getOrElse(-1).toString}")

    val keepSharesNum: Option[Int] = Await.result(
      SharesTable.insert(sharesToKeep.map(_.getShareTuple(TypeCode.Kept))),
      appConfig.slick.await.duration
    )
    log.info(s"keep: ${keepSharesNum.getOrElse(-1).toString}")
  }
}

object MonitorHandler {
  def apply()(implicit appConfig: AppConfig,
              investApiClient: BaseInvestApiClient) = new MonitorHandler[Void]()
}
