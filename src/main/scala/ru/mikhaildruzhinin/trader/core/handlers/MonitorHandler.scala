package ru.mikhaildruzhinin.trader.core.handlers

import ru.mikhaildruzhinin.trader.client.BaseInvestApiClient
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.mikhaildruzhinin.trader.core.ShareWrapper
import ru.mikhaildruzhinin.trader.core.TypeCode._
import ru.mikhaildruzhinin.trader.database.connection.{Connection, DatabaseConnection}
import ru.mikhaildruzhinin.trader.database.tables.shares.{SharesLogTable, SharesOperationsTable}

import scala.concurrent.Await

object MonitorHandler extends Handler {

  override def apply()(implicit appConfig: AppConfig,
                       investApiClient: BaseInvestApiClient,
                       connection: Connection): Int = {

    val purchasedShares: Seq[ShareWrapper] = ShareWrapper.getPersistedShares(Purchased)

    val (sharesToSell: Seq[ShareWrapper], sharesToKeep: Seq[ShareWrapper]) = investApiClient
      .getLastPrices(purchasedShares.map(_.figi))
      .zip(purchasedShares)
      .map(x => ShareWrapper(x._2, x._1))
      .partition(_.roi <= Some(BigDecimal(0)))

    val sellSharesNum: Seq[Option[Int]] = Await.result(
      DatabaseConnection.asyncRun(
        Vector(
          SharesOperationsTable.insert(sharesToSell.map(_.getShareTuple(Sold))),
          SharesLogTable.insert(sharesToSell.map(_.getShareTuple(Sold)))
        )
      ),
      appConfig.slick.await.duration
    )
    log.info(s"Sell: ${sellSharesNum.headOption.flatten.getOrElse(-1).toString}")

    val keepSharesNum: Int = Await
      .result(
        DatabaseConnection.asyncRun(
          Vector(
            SharesOperationsTable.insert(sharesToKeep.map(_.getShareTuple(Kept))),
            SharesLogTable.insert(sharesToKeep.map(_.getShareTuple(Kept)))
          )
        ),
        appConfig.slick.await.duration
      )
      .headOption
      .flatten
      .getOrElse(-1)

    log.info(s"Keep: ${keepSharesNum.toString}")

    keepSharesNum
  }
}
