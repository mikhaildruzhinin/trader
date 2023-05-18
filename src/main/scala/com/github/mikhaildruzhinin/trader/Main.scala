package com.github.mikhaildruzhinin.trader

import com.typesafe.scalalogging.Logger
import pureconfig.ConfigSource
import pureconfig.generic.auto.exportReader
import ru.tinkoff.piapi.core.{InstrumentsService, InvestApi, MarketDataService}

object Main extends App {
  val log: Logger = Logger(getClass.getName.stripSuffix("$"))

  implicit val config: Config = ConfigSource.default.loadOrThrow[Config]

  val token: String = config.tinkoffInvestApi.token
  val api: InvestApi = InvestApi.createSandbox(token)
  implicit val instrumentService: InstrumentsService = api.getInstrumentsService
  implicit val marketDataService: MarketDataService = api.getMarketDataService

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
