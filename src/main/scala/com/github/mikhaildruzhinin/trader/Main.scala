package com.github.mikhaildruzhinin.trader

import com.github.mikhaildruzhinin.trader.config.{AppConfig, ConfigReader}
import com.github.mikhaildruzhinin.trader.database.SharesTable
import com.typesafe.scalalogging.Logger

import java.util.concurrent.TimeUnit
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Main extends App {
  val log: Logger = Logger(getClass.getName.stripSuffix("$"))

  implicit val appConfig: AppConfig = ConfigReader.appConfig

  log.info("start")
  Await.ready(
    SharesTable.createIfNotExists,
    Duration(1, TimeUnit.MINUTES)
  )

  Await.result(
    SharesTable.filterByTypeCd(1),
    Duration(1, TimeUnit.MINUTES)
  ).foreach(println)

  implicit val investApiClient: InvestApiClient.type = InvestApiClient

  val shares: Seq[ShareWrapper] = ShareWrapper
    .getAvailableShares

  val sharesNum: Option[Int] = Await.result(
    SharesTable.insert(shares.map(_.getShareTuple(1))),
    Duration(1, TimeUnit.MINUTES)
  )
  log.info(s"total: ${sharesNum.getOrElse(-1).toString}")
//  wrappedShares.foreach(s => log.info(s.toString))

  val uptrendShares: Seq[ShareWrapper] = shares
    .map(_.updateShare)
    .filter(_.uptrendPct > Some(appConfig.uptrendThresholdPct))
    .sortBy(_.uptrendAbs)
    .reverse
    .take(appConfig.numUptrendShares)

  val uptrendSharesNum: Option[Int] = Await.result(
    SharesTable.insert(uptrendShares.map(_.getShareTuple(2))),
    Duration(1, TimeUnit.MINUTES)
  )
  log.info(s"best uptrend: ${uptrendSharesNum.getOrElse(-1).toString}")
//  wrappedSharesUptrend.foreach(s => log.info(s.toString))

  // buy uptrendShares
  val purchasedShares: Seq[ShareWrapper] = uptrendShares
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
    SharesTable.insert(purchasedShares.map(_.getShareTuple(3))),
    Duration(1, TimeUnit.MINUTES)
  )
  log.info(s"purchased: ${purchasedSharesNum.getOrElse(-1).toString}")

  val (sharesToSell: List[ShareWrapper], sharesToKeep: Seq[ShareWrapper]) = investApiClient
    .getLastPrices(purchasedShares.map(_.figi))
    .zip(purchasedShares)
    .map(x => ShareWrapper(x._2, x._1))
    .partition(_.isCheaperThanPurchasePrice)

  val sellSharesNum: Option[Int] = Await.result(
    SharesTable.insert(sharesToSell.map(_.getShareTuple(4))),
    Duration(1, TimeUnit.MINUTES)
  )
  log.info(s"sell: ${sellSharesNum.getOrElse(-1).toString}")
  sharesToSell.foreach(s => log.info(s.toString))

  val keepSharesNum: Option[Int] = Await.result(
    SharesTable.insert(sharesToKeep.map(_.getShareTuple(5))),
    Duration(1, TimeUnit.MINUTES)
  )
  log.info(s"keep: ${keepSharesNum.getOrElse(-1).toString}")
  sharesToKeep.foreach(s => log.info(s.toString))
}
