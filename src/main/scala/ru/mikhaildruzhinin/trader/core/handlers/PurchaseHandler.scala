package ru.mikhaildruzhinin.trader.core.handlers

import com.typesafe.scalalogging.Logger
import ru.mikhaildruzhinin.trader.client.BaseInvestApiClient
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.mikhaildruzhinin.trader.core.ShareWrapper
import ru.mikhaildruzhinin.trader.core.TypeCode._
import ru.mikhaildruzhinin.trader.database.connection.{Connection, DatabaseConnection}
import ru.mikhaildruzhinin.trader.database.tables.{SharesLogTable, SharesTable}

import scala.concurrent.Await

object PurchaseHandler extends Handler {

  val log: Logger = Logger(getClass.getName)

  private def loadAvailableShares()(implicit appConfig: AppConfig,
                                    investApiClient: BaseInvestApiClient): Seq[Option[Int]] = {

    val shares: Seq[ShareWrapper] = ShareWrapper
      .getAvailableShares

    val sharesNum: Seq[Option[Int]] = Await.result(
      DatabaseConnection.asyncRun(
        Vector(
          SharesTable.insert(shares.map(_.getShareTuple(Available))),
          SharesLogTable.insert(shares.map(_.getShareTuple(Available)))
        )
      ),
      appConfig.slick.await.duration
    )
    log.info(s"Total: ${sharesNum.headOption.flatten.getOrElse(-1).toString}")
    sharesNum
  }

  private def attemptLoadUptrendShares(numAttempt: Int,
                                       maxNumAttempts: Int,
                                       fallbackNumUptrendShares: Int)
                                      (implicit appConfig: AppConfig,
                                       investApiClient: BaseInvestApiClient,
                                       connection: Connection): Option[Int] = {

    if (numAttempt < maxNumAttempts) {
      Thread.sleep(5 * 60 * 1000)
      loadUptrendShares(numAttempt + 1)
    } else Some(fallbackNumUptrendShares)
  }

  private def loadUptrendShares(numAttempt: Int = 1)
                               (implicit appConfig: AppConfig,
                                investApiClient: BaseInvestApiClient,
                                connection: Connection): Option[Int] = {

    val maxNumAttempts: Int = 3
    log.info(s"Attempt $numAttempt of $maxNumAttempts")
    val uptrendShares: Seq[ShareWrapper] = ShareWrapper
      .getPersistedShares(Available)
      .map(_.updateShare)
//      .filter(_.uptrendPct >= Some(appConfig.shares.uptrendThresholdPct))
      .sortBy(_.uptrendAbs)
      .reverse
      .take(appConfig.shares.numUptrendShares)

    val uptrendSharesNum: Seq[Option[Int]] = Await.result(
      DatabaseConnection.asyncRun(
        Vector(
          SharesTable.insert(uptrendShares.map(_.getShareTuple(Uptrend))),
          SharesLogTable.insert(uptrendShares.map(_.getShareTuple(Uptrend)))
        )
      ),
      appConfig.slick.await.duration
    )
    log.info(s"Best uptrend: ${uptrendSharesNum.headOption.flatten.getOrElse(-1).toString}")

    uptrendSharesNum.headOption.flatten match {
      case Some(x) if x > 0 => Some(x)
      case Some(x) => attemptLoadUptrendShares(
        numAttempt = numAttempt,
        maxNumAttempts = maxNumAttempts,
        fallbackNumUptrendShares = x
      )
      case None => attemptLoadUptrendShares(
        numAttempt = numAttempt,
        maxNumAttempts = maxNumAttempts,
        fallbackNumUptrendShares = -1
      )
    }
  }

  private def purchaseShares()(implicit appConfig: AppConfig,
                               connection: Connection): Seq[Option[Int]] = {

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

    val purchasedSharesNum: Seq[Option[Int]] = Await.result(
      DatabaseConnection.asyncRun(
        Vector(
          SharesTable.insert(purchasedShares.map(_.getShareTuple(Purchased))),
          SharesLogTable.insert(purchasedShares.map(_.getShareTuple(Purchased)))
        )
      ),
      appConfig.slick.await.duration
    )
    log.info(s"Purchased: ${purchasedSharesNum.headOption.flatten.getOrElse(-1).toString}")
    purchasedSharesNum
  }

  override def apply()(implicit appConfig: AppConfig,
                       investApiClient: BaseInvestApiClient,
                       connection: Connection): Unit = {

    loadAvailableShares()
    loadUptrendShares()
    purchaseShares()
  }
}
