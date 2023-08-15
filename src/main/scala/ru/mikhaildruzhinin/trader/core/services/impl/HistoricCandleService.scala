package ru.mikhaildruzhinin.trader.core.services.impl

import ru.mikhaildruzhinin.trader.client.base.BaseInvestApiClient
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.mikhaildruzhinin.trader.core.services.base.BaseHistoricCandleService
import ru.mikhaildruzhinin.trader.core.wrappers.{HistoricCandleWrapper, ShareWrapper}
import ru.tinkoff.piapi.contract.v1.{CandleInterval, HistoricCandle}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class HistoricCandleService(investApiClient: BaseInvestApiClient)
                           (implicit appConfig: AppConfig) extends BaseHistoricCandleService {

  protected def getCandles(shares: Seq[ShareWrapper]): Future[Seq[HistoricCandle]] = Future
    .sequence(
      shares.map(s => investApiClient.getCandles(
        figi = s.figi,
        from = appConfig.exchange.startInstantFrom,
        to = appConfig.exchange.startInstantTo,
        interval = CandleInterval.CANDLE_INTERVAL_5_MIN
      ).map(_.head))
    )

  override def wrapCandles(candles: Seq[HistoricCandle]): Future[Seq[HistoricCandleWrapper]] = Future {
    candles.map(c => HistoricCandleWrapper(c))
  }

  override def getWrappedCandles(shares: Seq[ShareWrapper]): Future[Seq[HistoricCandleWrapper]] = for {
    candles <- getCandles(shares)
    wrappedCandles <- wrapCandles(candles)
  } yield wrappedCandles
}
