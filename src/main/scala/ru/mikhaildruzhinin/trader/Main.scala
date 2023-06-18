package ru.mikhaildruzhinin.trader

import com.typesafe.scalalogging.Logger
import ru.mikhaildruzhinin.trader.client.{BaseInvestApiClient, SyncInvestApiClient}
import ru.mikhaildruzhinin.trader.config.{AppConfig, ConfigReader}
import ru.mikhaildruzhinin.trader.core.ShareWrapper
import ru.mikhaildruzhinin.trader.core.TypeCode._
import ru.mikhaildruzhinin.trader.database.connection.{Connection, DatabaseConnection}
import ru.mikhaildruzhinin.trader.database.{SharesLogTable, SharesTable}

import scala.concurrent.Await

object Main extends App {

  val log: Logger = Logger(getClass.getName)
  log.info("Start")

  implicit val appConfig: AppConfig = ConfigReader.appConfig
  implicit val connection: Connection = DatabaseConnection
  implicit val investApiClient: BaseInvestApiClient = SyncInvestApiClient

   Await.result(
    DatabaseConnection.asyncRun(
      Vector(
        SharesTable.createIfNotExists,
        SharesLogTable.createIfNotExists
      )
    ),
    appConfig.slick.await.duration
  )

  val shares: Seq[ShareWrapper] = ShareWrapper
    .getAvailableShares

  val sharesNum: Seq[Option[Int]] = DatabaseConnection.run(
    Vector(
      SharesTable.insert(shares.map(_.getShareTuple(Available))),
      SharesLogTable.insert(shares.map(_.getShareTuple(Available)))
    )
  )
  log.info(s"Total: ${sharesNum.headOption.flatten.getOrElse(-1).toString}")

  val uptrendShares: Seq[ShareWrapper] = ShareWrapper
    .getPersistedShares(Available)
    .map(_.updateShare)
    .filter(_.uptrendPct >= Some(appConfig.shares.uptrendThresholdPct))
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

  val persistedPurchasedShares = ShareWrapper.getPersistedShares(Purchased)
  val (sharesToSell: List[ShareWrapper], sharesToKeep: Seq[ShareWrapper]) = investApiClient
    .getLastPrices(persistedPurchasedShares.map(_.figi))
    .zip(persistedPurchasedShares)
    .map(x => core.ShareWrapper(x._2, x._1))
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
  sharesToSell.foreach(s => log.info(s.toString))

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
  sharesToKeep.foreach(s => log.info(s.toString))
}
