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

  private def handleConfigReaderFailures(configReaderFailures: ConfigReaderFailures): Nothing = {
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

        val result = for {
          shares <- client.getShares()

          filteredShares <- Future {
            shares.filter(share => appConfig.exchanges.contains(share.exchange))
          }

          candles <- Future.sequence {
            Thread.sleep(1000L)
            filteredShares.map(share => {
              client.getCandles(
                share = share,
                from = Instant.now().minus(1, ChronoUnit.DAYS),
                to = Instant.now(),
                candleInterval = candleInterval
              )
            })
          }

          barSeries <- Future {
            filteredShares
              .zip(candles)
              .map(shareWithCandles =>
                new BaseBarSeriesBuilder()
                  .withName(shareWithCandles._1.name)
                  .withBars(shareWithCandles._2.flatMap(_.toBar).asJava)
                  .build()
              )
          }

          tradingStrategyResults <- Future.sequence(barSeries.map(bs => EmaCrossoverStrategy(bs)))

          _ <- Future(tradingStrategyResults.foreach(r => println(r)))
        } yield ()

        Await.result(result, Duration(10L, SECONDS))
      }
    )
  }
}
