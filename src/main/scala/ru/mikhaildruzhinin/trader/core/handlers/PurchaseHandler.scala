package ru.mikhaildruzhinin.trader.core.handlers

import com.github.kagkarlsson.scheduler.task.{ExecutionContext, TaskInstance, VoidExecutionHandler}
import com.typesafe.scalalogging.Logger
import ru.mikhaildruzhinin.trader.client.BaseInvestApiClient
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.mikhaildruzhinin.trader.core.{ShareWrapper, TypeCode}
import ru.mikhaildruzhinin.trader.database.SharesTable

import scala.concurrent.Await

class PurchaseHandler[T](implicit appConfig: AppConfig,
                         investApiClient: BaseInvestApiClient) extends VoidExecutionHandler[T] {
  val log: Logger = Logger(getClass.getName.stripSuffix("$"))

  override def execute(taskInstance: TaskInstance[T],
                       executionContext: ExecutionContext): Unit = {

    val shares: Seq[ShareWrapper] = ShareWrapper
      .getAvailableShares

    val sharesNum: Option[Int] = Await.result(
      SharesTable.insert(shares.map(_.getShareTuple(TypeCode.Available))),
      appConfig.slick.await.duration
    )
    log.info(s"total: ${sharesNum.getOrElse(-1).toString}")

    val uptrendShares: Seq[ShareWrapper] = ShareWrapper
      .getPersistedShares(TypeCode.Available)
      .map(_.updateShare)
      .filter(_.uptrendPct >= Some(appConfig.uptrendThresholdPct))
      .sortBy(_.uptrendAbs)
      .reverse
      .take(appConfig.numUptrendShares)

    val uptrendSharesNum: Option[Int] = Await.result(
      SharesTable.insert(uptrendShares.map(_.getShareTuple(TypeCode.Uptrend))),
      appConfig.slick.await.duration
    )
    log.info(s"best uptrend: ${uptrendSharesNum.getOrElse(-1).toString}")

    // buy uptrendShares
    val purchasedShares: Seq[ShareWrapper] = ShareWrapper
      .getPersistedShares(TypeCode.Uptrend)
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
      SharesTable.insert(purchasedShares.map(_.getShareTuple(TypeCode.Purchased))),
      appConfig.slick.await.duration
    )
    log.info(s"purchased: ${purchasedSharesNum.getOrElse(-1).toString}")
  }
}

object PurchaseHandler {
  def apply()(implicit appConfig: AppConfig,
              investApiClient: BaseInvestApiClient) = new PurchaseHandler[Void]()
}
