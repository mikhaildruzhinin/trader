package ru.mikhaildruzhinin.trader

import org.scalatest.funsuite.AnyFunSuite
import pureconfig.ConfigReader.Result
import pureconfig.error.ConfigReaderFailures
import pureconfig.generic.ProductHint
import pureconfig.generic.auto.exportReader
import pureconfig.{CamelCase, ConfigFieldMapping, ConfigSource}
import ru.tinkoff.piapi.contract.v1.{CandleInterval, InstrumentStatus}
import ru.tinkoff.piapi.core.InvestApi

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.jdk.CollectionConverters._

class Tests extends AnyFunSuite {

  val configResult: Result[AppConfig] = ConfigSource.default.load[AppConfig]

  implicit def hint[A]: ProductHint[A] = ProductHint[A](ConfigFieldMapping(CamelCase, CamelCase))

  private def handleConfigReaderFailures(configReaderFailures: ConfigReaderFailures): Unit = {
    println("Config is not valid. All errors:")
    configReaderFailures
      .toList
      .foreach(configReaderFailure => println(configReaderFailure.description))
    sys.exit(-1)
  }

  test("test shares") {
    configResult.fold(
      configReaderFailures => handleConfigReaderFailures(configReaderFailures),
      appConfig => assert(
        InvestApi.create(appConfig.tinkoffInvestApiToken)
          .getInstrumentsService
          .getSharesSync(InstrumentStatus.INSTRUMENT_STATUS_BASE)
          .asScala
          .toList
          .map(tinkoffShare => Share(tinkoffShare))
          .nonEmpty
      )
    )
  }

  test("test candles") {
    configResult.fold(
      configReaderFailures => handleConfigReaderFailures(configReaderFailures),
      appConfig => assert(
        InvestApi.create(appConfig.tinkoffInvestApiToken)
          .getMarketDataService
          .getCandlesSync(
            "BBG000QF1Q17",
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now(),
            CandleInterval.CANDLE_INTERVAL_5_MIN
          )
          .asScala
          .toList
          .map(historicCandle => Candle(historicCandle))
          .nonEmpty
      )
    )
  }
}
