package ru.mikhaildruzhinin.trader

import com.typesafe.scalalogging.Logger
import ru.mikhaildruzhinin.trader.client.{BaseInvestApiClient, SyncInvestApiClient}
import ru.mikhaildruzhinin.trader.config.{AppConfig, ConfigReader}
import ru.mikhaildruzhinin.trader.core.ShareWrapper
import ru.mikhaildruzhinin.trader.core.TypeCode._
import ru.mikhaildruzhinin.trader.database.SharesTable

import scala.concurrent.Await

object Main extends App {

  val log: Logger = Logger(getClass.getName)
  log.info("Start")

  implicit val appConfig: AppConfig = ConfigReader.appConfig

  Await.ready(
    SharesTable.createIfNotExists,
    appConfig.slick.await.duration
  )

  implicit val investApiClient: BaseInvestApiClient = SyncInvestApiClient

  val shares: Seq[ShareWrapper] = ShareWrapper
    .getAvailableShares

  val sharesNum: Option[Int] = Await.result(
    SharesTable.insert(shares.map(_.getShareTuple(Available))),
    appConfig.slick.await.duration
  )
  log.info(s"Total: ${sharesNum.getOrElse(-1).toString}")

  val uptrendShares: Seq[ShareWrapper] = ShareWrapper
    .getPersistedShares(Available)
    .map(_.updateShare)
    .filter(_.uptrendPct >= Some(appConfig.shares.uptrendThresholdPct))
    .sortBy(_.uptrendAbs)
    .reverse
    .take(appConfig.shares.numUptrendShares)

  val uptrendSharesNum: Option[Int] = Await.result(
    SharesTable.insert(uptrendShares.map(_.getShareTuple(Uptrend))),
    appConfig.slick.await.duration
  )
  log.info(s"Best uptrend: ${uptrendSharesNum.getOrElse(-1).toString}")

  // buy uptrendShares
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

  val purchasedSharesNum: Option[Int] = Await.result(
    SharesTable.insert(purchasedShares.map(_.getShareTuple(Purchased))),
    appConfig.slick.await.duration
  )
  log.info(s"Purchased: ${purchasedSharesNum.getOrElse(-1).toString}")

  val persistedPurchasedShares = ShareWrapper.getPersistedShares(Purchased)
  val (sharesToSell: List[ShareWrapper], sharesToKeep: Seq[ShareWrapper]) = investApiClient
    .getLastPrices(persistedPurchasedShares.map(_.figi))
    .zip(persistedPurchasedShares)
    .map(x => core.ShareWrapper(x._2, x._1))
    .partition(_.roi <= Some(BigDecimal(0)))

  val sellSharesNum: Option[Int] = Await.result(
    SharesTable.insert(sharesToSell.map(_.getShareTuple(Sold))),
    appConfig.slick.await.duration
  )
  log.info(s"Sell: ${sellSharesNum.getOrElse(-1).toString}")
  sharesToSell.foreach(s => log.info(s.toString))

  val keepSharesNum: Option[Int] = Await.result(
    SharesTable.insert(sharesToKeep.map(_.getShareTuple(Kept))),
    appConfig.slick.await.duration
  )
  log.info(s"Keep: ${keepSharesNum.getOrElse(-1).toString}")
  sharesToKeep.foreach(s => log.info(s.toString))
}
