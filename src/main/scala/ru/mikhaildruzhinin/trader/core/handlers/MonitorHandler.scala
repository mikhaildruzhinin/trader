package ru.mikhaildruzhinin.trader.core.handlers

import ru.mikhaildruzhinin.trader.client.BaseInvestApiClient
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.mikhaildruzhinin.trader.core.ShareWrapper
import ru.mikhaildruzhinin.trader.core.TypeCode._
import ru.mikhaildruzhinin.trader.database.connection.{Connection, DatabaseConnection}
import ru.mikhaildruzhinin.trader.database.tables.{SharesLogTable, SharesTable}

import scala.concurrent.Await

object MonitorHandler extends Handler {

  override def apply()(implicit appConfig: AppConfig,
                       investApiClient: BaseInvestApiClient,
                       connection: Connection): Unit = {

    val purchasedShares: Seq[ShareWrapper] = ShareWrapper.getPersistedShares(Purchased)

    val (sharesToSell: Seq[ShareWrapper], sharesToKeep: Seq[ShareWrapper]) = investApiClient
      .getLastPrices(purchasedShares.map(_.figi))
      .zip(purchasedShares)
      .map(x => ShareWrapper(x._2, x._1))
      .partition(_.roi <= Some(BigDecimal(0)))

    val sellSharesNum: Seq[Option[Int]] = Await.result(
      DatabaseConnection.asyncRun(
        Vector(
          SharesTable.insert(sharesToSell.map(_.getShareTuple(Sold))),
          SharesLogTable.insert(sharesToSell.map(_.getShareTuple(Sold)))
        )
      ),
      appConfig.slick.await.duration
    )
    log.info(s"Sell: ${sellSharesNum.headOption.flatten.getOrElse(-1).toString}")

    val keepSharesNum: Seq[Option[Int]] = Await.result(
      DatabaseConnection.asyncRun(
        Vector(
          SharesTable.insert(sharesToKeep.map(_.getShareTuple(Kept))),
          SharesLogTable.insert(sharesToKeep.map(_.getShareTuple(Kept)))
        )
      ),
      appConfig.slick.await.duration
    )
    log.info(s"Keep: ${keepSharesNum.headOption.flatten.getOrElse(-1).toString}")
  }
}
