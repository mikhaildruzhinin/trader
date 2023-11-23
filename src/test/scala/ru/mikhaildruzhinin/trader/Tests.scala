package ru.mikhaildruzhinin.trader

import org.scalatest.funsuite.AnyFunSuite
import org.ta4j.core._
import org.ta4j.core.criteria.pnl.GrossReturnCriterion
import org.ta4j.core.indicators.EMAIndicator
import org.ta4j.core.indicators.helpers.ClosePriceIndicator
import org.ta4j.core.num.{DecimalNum, Num}
import org.ta4j.core.rules._
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

trait TradingStrategy {
  def apply(barSeries: BarSeries): Future[TradingStrategyResult]
}

object EmaCrossoverStrategy extends TradingStrategy {
  override def apply(barSeries: BarSeries): Future[TradingStrategyResult] = Future {
    val closePriceIndicator = new ClosePriceIndicator(barSeries)
    val shortEma = new EMAIndicator(closePriceIndicator, 20)
    val longEma = new EMAIndicator(closePriceIndicator, 50)

    val entryRule = new CrossedUpIndicatorRule(shortEma, longEma)

    val exitRule = new CrossedDownIndicatorRule(shortEma, longEma)
      .or(new StopGainRule(closePriceIndicator, 2))
      .or(new StopLossRule(closePriceIndicator, 3))

    val strategy: Strategy = new BaseStrategy(entryRule, exitRule)

    val seriesManager = new BarSeriesManager(barSeries)
    val tradingRecord: TradingRecord = seriesManager.run(strategy)

    val criterion = new GrossReturnCriterion()
    val criterionValue: Num = criterion.calculate(barSeries, tradingRecord)
      .multipliedBy(DecimalNum.valueOf(100))
    TradingStrategyResult(tradingRecord, criterionValue)
  }
}

case class TradingStrategyResult(tradingRecord: TradingRecord,
                                 criterionValue: Num) {
  override def toString: String = super.toString // TODO: override
}
