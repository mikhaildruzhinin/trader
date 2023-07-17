package ru.mikhaildruzhinin.trader.core.handlers

import ru.mikhaildruzhinin.trader.client.BaseInvestApiClient
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.mikhaildruzhinin.trader.core.ShareWrapper
import ru.mikhaildruzhinin.trader.core.TypeCode._
import ru.mikhaildruzhinin.trader.database.connection.{Connection, DatabaseConnection}
import ru.mikhaildruzhinin.trader.database.tables.shares.{SharesLogTable, SharesOperationsTable}

import scala.concurrent.Await

object PurchaseHandler extends Handler {

  private def loadAvailableShares()(implicit appConfig: AppConfig,
                                    investApiClient: BaseInvestApiClient): Int = {

    val shares: Seq[ShareWrapper] = ShareWrapper
      .getAvailableShares

    val sharesNum: Int = Await
      .result(
        DatabaseConnection.asyncRun(
          Vector(
            SharesOperationsTable.insert(shares.map(_.getShareTuple(Available))),
            SharesLogTable.insert(shares.map(_.getShareTuple(Available)))
          )
        ),
        appConfig.slick.await.duration
      )
      .headOption
      .flatten
      .getOrElse(-1)

    log.info(s"Total: ${sharesNum.toString}")
    sharesNum
  }

  private def attemptLoadUptrendShares(numAttempt: Int,
                                       maxNumAttempts: Int,
                                       fallbackNumUptrendShares: Int)
                                      (implicit appConfig: AppConfig,
                                       investApiClient: BaseInvestApiClient,
                                       connection: Connection): Int = {

    if (numAttempt < maxNumAttempts) {
      Thread.sleep(5 * 60 * 1000)
      loadUptrendShares(numAttempt + 1)
    } else fallbackNumUptrendShares
  }

  private def loadUptrendShares(numAttempt: Int = 1)
                               (implicit appConfig: AppConfig,
                                investApiClient: BaseInvestApiClient,
                                connection: Connection): Int = {

    val maxNumAttempts: Int = 3
    log.info(s"Attempt $numAttempt of $maxNumAttempts")
    val uptrendShares: Seq[ShareWrapper] = ShareWrapper
      .getPersistedShares(Available)
      .map(_.updateShare)
//      .filter(_.uptrendPct >= Some(appConfig.shares.uptrendThresholdPct))
      .sortBy(_.uptrendAbs)
      .reverse
      .take(appConfig.shares.numUptrendShares)

    val uptrendSharesNum: Int = Await
      .result(
        DatabaseConnection.asyncRun(
          Vector(
            SharesOperationsTable.insert(uptrendShares.map(_.getShareTuple(Uptrend))),
            SharesLogTable.insert(uptrendShares.map(_.getShareTuple(Uptrend)))
          )
        ),
        appConfig.slick.await.duration
      )
      .headOption
      .flatten
      .getOrElse(-1)

    log.info(s"Best uptrend: ${uptrendSharesNum.toString}")

    uptrendSharesNum match {
      case x if x > 0 => x
      case x => attemptLoadUptrendShares(
        numAttempt = numAttempt,
        maxNumAttempts = maxNumAttempts,
        fallbackNumUptrendShares = x
      )
    }
  }

  private def purchaseShares()(implicit appConfig: AppConfig,
                               connection: Connection): Int = {

    val purchasedShares: Seq[ShareWrapper] = ShareWrapper
      .getPersistedShares(Uptrend)
      .map(
        s => ShareWrapper(
          shareWrapper = s,
          startingPrice = s.startingPrice,
          purchasePrice = s.currentPrice,
          currentPrice = s.currentPrice,
          updateTime = s.updateTime
        )
      )

    val purchasedSharesNum: Int = Await
      .result(
        DatabaseConnection.asyncRun(
          Vector(
            SharesOperationsTable.insert(purchasedShares.map(_.getShareTuple(Purchased))),
            SharesLogTable.insert(purchasedShares.map(_.getShareTuple(Purchased)))
          )
        ),
        appConfig.slick.await.duration
      )
      .headOption
      .flatten
      .getOrElse(-1)

    log.info(s"Purchased: ${purchasedSharesNum.toString}")
    purchasedSharesNum
  }

  override def apply()(implicit appConfig: AppConfig,
                       investApiClient: BaseInvestApiClient,
                       connection: Connection): Int = {

    loadAvailableShares()
    loadUptrendShares()
    purchaseShares()
  }
}
