package com.github.mikhaildruzhinin.trader

import com.typesafe.scalalogging.Logger
import pureconfig.generic.auto.exportReader
import pureconfig.generic.semiauto.deriveEnumerationReader
import pureconfig.{ConfigReader, ConfigSource}

object Main extends App {
  val log: Logger = Logger(getClass.getName.stripSuffix("$"))

  implicit val investApiModeConvert: ConfigReader[InvestApiMode] = deriveEnumerationReader[InvestApiMode]
  implicit val config: Config = ConfigSource.default.loadOrThrow[Config]

  implicit val investApiClient: InvestApiClient.type = InvestApiClient

  val shareWrapper = ShareWrapper

  val wrappedShares: Iterator[ShareWrapper] = shareWrapper.getShares

  val wrappedSharesUptrend: List[ShareWrapper] = shareWrapper
    .getUptrendShares(
      wrappedShares,
      config.exchange.uptrendCheckTimedeltaHours
    )
    .filter(_.uptrendPct > Some(config.uptrendThresholdPct))
    .toList
    .sortBy(_.uptrendAbs)
    .reverse
    .take(config.numUptrendShares)

  wrappedSharesUptrend.foreach(s => log.info(s.toString))
}
