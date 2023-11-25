package ru.mikhaildruzhinin.trader

import org.ta4j.core.criteria.pnl.GrossReturnCriterion
import org.ta4j.core.indicators.EMAIndicator
import org.ta4j.core.indicators.helpers.ClosePriceIndicator
import org.ta4j.core.num.{DecimalNum, Num}
import org.ta4j.core.rules._
import org.ta4j.core._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class TradingStrategyResult(tradingRecord: TradingRecord,
                                 criterionValue: Num)

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
    val criterionValue: Num = criterion
      .calculate(barSeries, tradingRecord)
      .multipliedBy(DecimalNum.valueOf(100))
    TradingStrategyResult(tradingRecord, criterionValue)
  }
}
