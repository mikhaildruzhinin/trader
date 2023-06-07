package ru.mikhaildruzhinin.trader

import com.typesafe.scalalogging.Logger
import ru.mikhaildruzhinin.trader.client.{BaseInvestApiClient, SyncInvestApiClient}
import ru.mikhaildruzhinin.trader.config.{AppConfig, ConfigReader}
import ru.mikhaildruzhinin.trader.core.{ShareWrapper, TypeCode}
import ru.mikhaildruzhinin.trader.database.SharesTable

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
    SharesTable.insert(shares.map(_.getShareTuple(TypeCode.Available))),
    appConfig.slick.await.duration
  )
  log.info(s"total: ${sharesNum.getOrElse(-1).toString}")
//  wrappedShares.foreach(s => log.info(s.toString))

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
//  wrappedSharesUptrend.foreach(s => log.info(s.toString))

  // buy uptrendShares
  val purchasedShares: Seq[ShareWrapper] = ShareWrapper
    .getPersistedShares(TypeCode.Uptrend)
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
    SharesTable.insert(purchasedShares.map(_.getShareTuple(TypeCode.Purchased))),
    appConfig.slick.await.duration
  )
  log.info(s"purchased: ${purchasedSharesNum.getOrElse(-1).toString}")

  val (sharesToSell: List[ShareWrapper], sharesToKeep: Seq[ShareWrapper]) = investApiClient
    .getLastPrices(ShareWrapper.getPersistedShares(TypeCode.Purchased).map(_.figi))
    .zip(ShareWrapper.getPersistedShares(TypeCode.Purchased))
    .map(x => core.ShareWrapper(x._2, x._1))
    .partition(_.roi <= Some(BigDecimal(0)))

  val sellSharesNum: Option[Int] = Await.result(
    SharesTable.insert(sharesToSell.map(_.getShareTuple(TypeCode.Sold))),
    appConfig.slick.await.duration
  )
  log.info(s"sell: ${sellSharesNum.getOrElse(-1).toString}")
  sharesToSell.foreach(s => log.info(s.toString))

  val keepSharesNum: Option[Int] = Await.result(
    SharesTable.insert(sharesToKeep.map(_.getShareTuple(TypeCode.Kept))),
    appConfig.slick.await.duration
  )
  log.info(s"keep: ${keepSharesNum.getOrElse(-1).toString}")
  sharesToKeep.foreach(s => log.info(s.toString))
}
