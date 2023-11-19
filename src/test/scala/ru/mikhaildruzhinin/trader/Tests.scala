package ru.mikhaildruzhinin.trader

import org.scalatest.funsuite.AnyFunSuite
import org.ta4j.core.BaseBarSeriesBuilder
import pureconfig.ConfigReader.Result
import pureconfig.error.ConfigReaderFailures
import pureconfig.generic.ProductHint
import pureconfig.generic.auto.exportReader
import pureconfig.{CamelCase, ConfigFieldMapping, ConfigSource}
import ru.tinkoff.piapi.contract.v1.CandleInterval

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, SECONDS}
import scala.concurrent.{Await, Future}
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

  test("test bar series") {
    configResult.fold(
      configReaderFailures => handleConfigReaderFailures(configReaderFailures),
      appConfig => {

        val client = InvestApiClientImpl(appConfig)

        val candleInterval = CandleInterval.CANDLE_INTERVAL_5_MIN

        val barSeries = for {
          shares <- client.getShares()

          filteredShares <- Future {
            shares.filter(share => appConfig.exchanges.contains(share.exchange))
          }

          candles <- Future.sequence {
            Thread.sleep(5000L)
            filteredShares.map(share => {
              client.getCandles(
                share = share,
                from = Instant.now().minus(1, ChronoUnit.DAYS),
                to = Instant.now(),
                candleInterval = candleInterval
              )
            })
          }

          bars <- Future(candles.map(_.flatMap(_.toBar)))

          barSeries <- Future {
            filteredShares.zip(bars)
              .map(shareWithBars => new BaseBarSeriesBuilder()
                .withName(shareWithBars._1.name)
                .withBars(shareWithBars._2.asJava)
                .build())
          }
        } yield barSeries

        Await.result(barSeries, Duration(10L, SECONDS))
          .foreach(barSeries => println(barSeries.getName, barSeries.getBarCount))
      }
    )
  }

}
