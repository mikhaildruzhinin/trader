package com.github.mikhaildruzhinin.trader

import com.github.mikhaildruzhinin.trader.config.{AppConfig, InvestApiMode}
import com.typesafe.scalalogging.Logger
import pureconfig.generic.auto.exportReader
import pureconfig.generic.semiauto.deriveEnumerationReader
import pureconfig.{ConfigReader, ConfigSource}

object Main extends App {
  val log: Logger = Logger(getClass.getName.stripSuffix("$"))

  log.info("start")
  implicit val investApiModeConvert: ConfigReader[InvestApiMode] = deriveEnumerationReader[InvestApiMode]
  implicit val appConfig: AppConfig = ConfigSource.default.loadOrThrow[AppConfig]

  implicit val investApiClient: InvestApiClient.type = InvestApiClient

  val wrappedSharesUptrend: List[ShareWrapper] = ShareWrapper
    .getAvailableShares
    .map(_.updateShare)
    .filter(_.uptrendPct > Some(appConfig.uptrendThresholdPct))
    .sortBy(_.uptrendAbs)
    .reverse
    .take(appConfig.numUptrendShares)

  wrappedSharesUptrend.foreach(s => log.info(s.toString))
  log.info(wrappedSharesUptrend.length.toString)

  // buy wrappedSharesUptrend
  val purchasedShares: List[ShareWrapper] = wrappedSharesUptrend
    .map(
      s => ShareWrapper(
        shareWrapper = s,
        startingPrice = s.startingPrice,
        purchasePrice = s.currentPrice,
        currentPrice = s.currentPrice,
        updateTime = s.updateTime
      )
    )

  val (sharesToSell: List[ShareWrapper], sharesToKeep: List[ShareWrapper]) = investApiClient
    .getLastPrices(purchasedShares.map(_.figi))
    .zip(purchasedShares)
    .map(x => ShareWrapper(x._2, x._1))
    .partition(_.isCheaperThanPurchasePrice)

  log.info(s"sell: ${sharesToSell.length}")
  sharesToSell.foreach(s => log.info(s.toString))

  log.info(s"keep: ${sharesToKeep.length}")
  sharesToKeep.foreach(s => log.info(s.toString))
}
