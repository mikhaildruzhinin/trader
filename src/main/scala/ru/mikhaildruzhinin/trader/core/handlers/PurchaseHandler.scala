package ru.mikhaildruzhinin.trader.core.handlers

import ru.mikhaildruzhinin.trader.client.BaseInvestApiClient
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.mikhaildruzhinin.trader.core.ShareWrapper
import ru.mikhaildruzhinin.trader.core.TypeCode._
import ru.mikhaildruzhinin.trader.database.connection.Connection
import ru.mikhaildruzhinin.trader.database.tables.SharesTable

import scala.concurrent.Await

object PurchaseHandler extends Handler {

  def loadAvailableShares()(implicit appConfig: AppConfig,
                                    investApiClient: BaseInvestApiClient,
                                    connection: Connection): Int = {

    val shares: Seq[ShareWrapper] = ShareWrapper
      .getAvailableShares

    shares.foreach(s => println(s.startingPrice))

    val sharesNum = Await.result(
      connection.asyncRun(
        Vector(
          SharesTable.delete(),
          SharesTable.insert(shares.map(_.getShareTuple(Available))),
        )
      ),
      appConfig.slick.await.duration
    )

    log.info(s"Total: ${shares.length.toString}")
    shares.length
  }

  def attemptLoadUptrendShares(numAttempt: Int,
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

    Await.result(
      connection.asyncRun(
        Vector(
          SharesTable.update(figis = uptrendShares.map(s => s.figi), Uptrend.code),
        )
      ),
      appConfig.slick.await.duration
    )

    log.info(s"Best uptrend: ${uptrendShares.length.toString}")

    uptrendShares.length match {
      case x if x > 0 => x
      case x => attemptLoadUptrendShares(
        numAttempt = numAttempt,
        maxNumAttempts = maxNumAttempts,
        fallbackNumUptrendShares = x
      )
    }
  }

  def purchaseShares()(implicit appConfig: AppConfig,
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

    Await.result(
      connection.asyncRun(
        Vector(
          SharesTable.update(figis = purchasedShares.map(s => s.figi), Purchased.code),
        )
      ),
      appConfig.slick.await.duration
    )

    log.info(s"Purchased: ${purchasedShares.length.toString}")
    purchasedShares.length
  }

  override def apply()(implicit appConfig: AppConfig,
                       investApiClient: BaseInvestApiClient,
                       connection: Connection): Int = {

    loadAvailableShares()
    loadUptrendShares()
    purchaseShares()
  }
}
