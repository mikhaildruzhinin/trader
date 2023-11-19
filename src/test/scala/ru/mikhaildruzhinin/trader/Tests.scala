package ru.mikhaildruzhinin.trader

import org.scalatest.funsuite.AnyFunSuite
import org.ta4j.core.BaseBarSeriesBuilder
import pureconfig.ConfigReader.Result
import pureconfig.error.ConfigReaderFailures
import pureconfig.generic.ProductHint
import pureconfig.generic.auto.exportReader
import pureconfig.{CamelCase, ConfigFieldMapping, ConfigSource}
import ru.tinkoff.piapi.contract.v1.{CandleInterval, InstrumentStatus}
import ru.tinkoff.piapi.core.InvestApi

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, SECONDS}
import scala.concurrent.{Await, Future}
import scala.jdk.CollectionConverters._
import scala.jdk.FutureConverters.CompletionStageOps

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
    val candleInterval = CandleInterval.CANDLE_INTERVAL_5_MIN

    configResult.fold(
      configReaderFailures => handleConfigReaderFailures(configReaderFailures),
      appConfig => assert(
        InvestApi.create(appConfig.tinkoffInvestApiToken)
          .getMarketDataService
          .getCandlesSync(
            "BBG000QF1Q17",
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now(),
            candleInterval
          )
          .asScala
          .toList
          .map(historicCandle => Candle(historicCandle, candleInterval))
          .nonEmpty
      )
    )
  }

  test("test bar series") {
    configResult.fold(
      configReaderFailures => handleConfigReaderFailures(configReaderFailures),
      appConfig => {

        val candleInterval = CandleInterval.CANDLE_INTERVAL_5_MIN

        val barSeries = for {
          tinkoffShares <- InvestApi
            .create(appConfig.tinkoffInvestApiToken)
            .getInstrumentsService
            .getShares(InstrumentStatus.INSTRUMENT_STATUS_BASE)
            .asScala
            .map(_.asScala.toList)

          shares <- Future {
            tinkoffShares
              .map(tinkoffShare => Share(tinkoffShare))
              .filter(share => appConfig.exchanges.contains(share.exchange))
          }

          historicCandles <- Future.sequence {
            Thread.sleep(5000L)

            shares.map(share => {
              InvestApi
                .create(appConfig.tinkoffInvestApiToken)
                .getMarketDataService
                .getCandles(
                  share.figi,
                  Instant.now().minus(1, ChronoUnit.DAYS),
                  Instant.now(),
                  candleInterval
                )
                .asScala
                .map(_.asScala.toList)
            })
          }

          bars <- Future {
            historicCandles
              .map(_.flatMap(historicCandle => Candle(historicCandle, candleInterval).toBar))
          }

          barSeries <- Future {
            bars.map(b => new BaseBarSeriesBuilder()
              .withName("share_candles")
              .withBars(b.asJava)
              .build())
          }
        } yield barSeries

        Await.result(barSeries, Duration(10L, SECONDS))
          .map(_.getBarData.asScala.foreach(println))
      }
    )
  }

}
