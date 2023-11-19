package ru.mikhaildruzhinin.trader

import org.scalatest.funsuite.AnyFunSuite
import pureconfig.ConfigReader.Result
import pureconfig.error.ConfigReaderFailures
import pureconfig.generic.ProductHint
import pureconfig.generic.auto.exportReader
import pureconfig.{CamelCase, ConfigFieldMapping, ConfigSource}
import ru.tinkoff.piapi.contract.v1.InstrumentStatus
import ru.tinkoff.piapi.core.InvestApi

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
          .map(s => Share(s))
          .nonEmpty
      )
    )
  }
}
