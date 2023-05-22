package com.github.mikhaildruzhinin.trader

import com.github.mikhaildruzhinin.trader.config.{Config, InvestApiMode}
import com.typesafe.scalalogging.Logger
import pureconfig.generic.auto.exportReader
import pureconfig.generic.semiauto.deriveEnumerationReader
import pureconfig.{ConfigReader, ConfigSource}
import ru.tinkoff.piapi.contract.v1.Quotation
import ru.tinkoff.piapi.core.utils.MapperUtils.quotationToBigDecimal

import scala.jdk.CollectionConverters._
import scala.math.BigDecimal.{RoundingMode, javaBigDecimal2bigDecimal}

object Main extends App {
  val log: Logger = Logger(getClass.getName.stripSuffix("$"))

  implicit val investApiModeConvert: ConfigReader[InvestApiMode] = deriveEnumerationReader[InvestApiMode]
  implicit val config: Config = ConfigSource.default.loadOrThrow[Config]

  implicit val investApiClient: InvestApiClient.type = InvestApiClient

  val wrappedSharesUptrend: List[ShareWrapper] = ShareWrapper
    .getAvailableShares
    .map(_.updateShare)
    .filter(_.uptrendPct > Some(config.uptrendThresholdPct))
    .toList
    .sortBy(_.uptrendAbs)
    .reverse
    .take(config.numUptrendShares)

  wrappedSharesUptrend.foreach(s => log.info(s.toString))

  // buy wrappedSharesUptrend
  val purchasedShares: List[ShareWrapper] = wrappedSharesUptrend.map(
    s => {
      ShareWrapper(s, s.startingPrice, s.currentPrice, s.currentPrice, s.updateTime)
    }
  )

  val r = investApiClient
    .getLastPrices(wrappedSharesUptrend.map(_.figi))
    .zip(wrappedSharesUptrend)
    .toList
    .partition(
      x => {
        quotationToBigDecimal(x._1.getPrice) >=
          quotationToBigDecimal(
            x._2.currentPrice
              .getOrElse(
                Quotation
                  .newBuilder()
                  .build()
              )
          )
      }
    )

  val sharesToSell: List[ShareWrapper] = r._1.map(_._2)
  r._1.foreach(
    x => println(
      x._2.name,
      quotationToBigDecimal(x._1.getPrice),
      quotationToBigDecimal(
        x._2.currentPrice
          .getOrElse(
            Quotation
              .newBuilder()
              .build()
          )
      )
    )
  )

  // sell sharesToSell

  val sharesToKeep: List[ShareWrapper] = r._2.map(_._2)
  sharesToKeep.foreach(s => println(s.name))
  // sell sharesToSell
}
