package com.github.mikhaildruzhinin.trader

import com.github.mikhaildruzhinin.trader.config.{AppConfig, InvestApiMode}
import com.github.mikhaildruzhinin.trader.database.{Connection, Models}
import com.typesafe.scalalogging.Logger
import pureconfig.generic.ProductHint
import pureconfig.generic.auto.exportReader
import pureconfig.generic.semiauto.deriveEnumerationReader
import pureconfig.{CamelCase, ConfigFieldMapping, ConfigReader, ConfigSource}
import slick.jdbc.PostgresProfile.api._

import java.util.concurrent.TimeUnit
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Main extends App {
  implicit def hint[A]: ProductHint[A] = ProductHint[A](ConfigFieldMapping(CamelCase, CamelCase))

  val log: Logger = Logger(getClass.getName.stripSuffix("$"))

  log.info("start")
  Await.ready(
    Connection.db.run(Models.ddl.createIfNotExists),
    Duration(5, TimeUnit.MINUTES)
  )

  implicit val investApiModeConvert: ConfigReader[InvestApiMode] = deriveEnumerationReader[InvestApiMode]
  implicit val appConfig: AppConfig = ConfigSource.default.loadOrThrow[AppConfig]

  implicit val investApiClient: InvestApiClient.type = InvestApiClient

  val wrappedShares: Seq[ShareWrapper] = ShareWrapper
    .getAvailableShares

  val sharesNum: Option[Int] = Await.result(
    Connection.db.run(Models.insertShares(wrappedShares, "tst")),
    Duration(5, TimeUnit.MINUTES)
  )

  log.info(s"total: ${sharesNum.getOrElse(-1).toString}")
//  wrappedShares.foreach(s => log.info(s.toString))

  val wrappedSharesUptrend: Seq[ShareWrapper] = wrappedShares
    .map(_.updateShare)
    .filter(_.uptrendPct > Some(appConfig.uptrendThresholdPct))
    .sortBy(_.uptrendAbs)
    .reverse
    .take(appConfig.numUptrendShares)

  val uptrendSharesNum: Option[Int] = Await.result(
    Connection.db.run(Models.insertShares(wrappedSharesUptrend, "tst")),
    Duration(5, TimeUnit.MINUTES)
  )

  log.info(s"best uptrend: ${uptrendSharesNum.getOrElse(-1).toString}")
//  wrappedSharesUptrend.foreach(s => log.info(s.toString))

  // buy wrappedSharesUptrend
  val purchasedShares: Seq[ShareWrapper] = wrappedSharesUptrend
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
    Connection.db.run(Models.insertShares(purchasedShares, "tst")),
    Duration(5, TimeUnit.MINUTES)
  )

  log.info(s"purchased: ${purchasedSharesNum.getOrElse(-1).toString}")

  val (sharesToSell: List[ShareWrapper], sharesToKeep: Seq[ShareWrapper]) = investApiClient
    .getLastPrices(purchasedShares.map(_.figi))
    .zip(purchasedShares)
    .map(x => ShareWrapper(x._2, x._1))
    .partition(_.isCheaperThanPurchasePrice)

  val sellSharesNum: Option[Int] = Await.result(
    Connection.db.run(Models.insertShares(sharesToSell, "tst")),
    Duration(5, TimeUnit.MINUTES)
  )

  log.info(s"sell: ${sellSharesNum.getOrElse(-1).toString}")
  sharesToSell.foreach(s => log.info(s.toString))

  val keepSharesNum: Option[Int] = Await.result(
    Connection.db.run(Models.insertShares(sharesToKeep, "tst")),
    Duration(5, TimeUnit.MINUTES)
  )

  log.info(s"keep: ${keepSharesNum.getOrElse(-1).toString}")
  sharesToKeep.foreach(s => log.info(s.toString))
}
