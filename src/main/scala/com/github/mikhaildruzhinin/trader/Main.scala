package com.github.mikhaildruzhinin.trader

import com.github.mikhaildruzhinin.trader.client._
import com.github.mikhaildruzhinin.trader.config.{AppConfig, ConfigReader}
import com.github.mikhaildruzhinin.trader.core.ShareWrapper
import com.github.mikhaildruzhinin.trader.database.SharesTable
import com.typesafe.scalalogging.Logger

import scala.concurrent.Await

object Main extends App {

  val log: Logger = Logger(getClass.getName.stripSuffix("$"))
  log.info("start")

  implicit val appConfig: AppConfig = ConfigReader.appConfig

  Await.ready(
    SharesTable.createIfNotExists,
    appConfig.slick.await.duration
  )

  implicit val investApiClient: BaseInvestApiClient = SyncInvestApiClient

  val shares: Seq[ShareWrapper] = ShareWrapper
    .getAvailableShares

  val sharesNum: Option[Int] = Await.result(
    SharesTable.insert(shares.map(_.getShareTuple(1))),
    appConfig.slick.await.duration
  )
  log.info(s"total: ${sharesNum.getOrElse(-1).toString}")
//  wrappedShares.foreach(s => log.info(s.toString))

  val uptrendShares: Seq[ShareWrapper] = ShareWrapper
    .getPersistedShares(1)
    .map(_.updateShare)
    .filter(_.uptrendPct >= Some(appConfig.uptrendThresholdPct))
    .sortBy(_.uptrendAbs)
    .reverse
    .take(appConfig.numUptrendShares)

  val uptrendSharesNum: Option[Int] = Await.result(
    SharesTable.insert(uptrendShares.map(_.getShareTuple(2))),
    appConfig.slick.await.duration
  )
  log.info(s"best uptrend: ${uptrendSharesNum.getOrElse(-1).toString}")
//  wrappedSharesUptrend.foreach(s => log.info(s.toString))

  // buy uptrendShares
  val purchasedShares: Seq[ShareWrapper] = ShareWrapper
    .getPersistedShares(2)
    .map(
      s => core.ShareWrapper(
        shareWrapper = s,
        startingPrice = s.startingPrice,
        purchasePrice = s.currentPrice,
        currentPrice = s.currentPrice,
        updateTime = s.updateTime
      )
    )

  val purchasedSharesNum: Option[Int] = Await.result(
    SharesTable.insert(purchasedShares.map(_.getShareTuple(3))),
    appConfig.slick.await.duration
  )
  log.info(s"purchased: ${purchasedSharesNum.getOrElse(-1).toString}")

  val (sharesToSell: List[ShareWrapper], sharesToKeep: Seq[ShareWrapper]) = investApiClient
    .getLastPrices(ShareWrapper.getPersistedShares(3).map(_.figi))
    .zip(ShareWrapper.getPersistedShares(3))
    .map(x => core.ShareWrapper(x._2, x._1))
    .partition(_.roi <= Some(BigDecimal(0)))

  val sellSharesNum: Option[Int] = Await.result(
    SharesTable.insert(sharesToSell.map(_.getShareTuple(4))),
    appConfig.slick.await.duration
  )
  log.info(s"sell: ${sellSharesNum.getOrElse(-1).toString}")
  sharesToSell.foreach(s => log.info(s.toString))

  val keepSharesNum: Option[Int] = Await.result(
    SharesTable.insert(sharesToKeep.map(_.getShareTuple(5))),
    appConfig.slick.await.duration
  )
  log.info(s"keep: ${keepSharesNum.getOrElse(-1).toString}")
  sharesToKeep.foreach(s => log.info(s.toString))
}
