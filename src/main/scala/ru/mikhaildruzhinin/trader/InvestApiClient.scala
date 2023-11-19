package ru.mikhaildruzhinin.trader

import ru.tinkoff.piapi.contract.v1.{CandleInterval, InstrumentStatus}
import ru.tinkoff.piapi.core.{InstrumentsService, InvestApi, MarketDataService}

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.jdk.CollectionConverters._
import scala.jdk.FutureConverters.CompletionStageOps

trait InvestApiClient {
  def getShares(instrumentStatus: InstrumentStatus): Future[List[Share]]

  def getCandles(share: Share,
                 from: Instant,
                 to: Instant,
                 candleInterval: CandleInterval): Future[List[Candle]]
}

class InvestApiClientImpl(token: String) extends InvestApiClient {
  protected val investApi: InvestApi = InvestApi.create(token)

  protected val instrumentService: InstrumentsService = investApi.getInstrumentsService

  protected val marketDataService: MarketDataService = investApi.getMarketDataService

  override def getShares(instrumentStatus: InstrumentStatus = InstrumentStatus.INSTRUMENT_STATUS_BASE): Future[List[Share]] = {
    instrumentService
      .getShares(instrumentStatus)
      .asScala
      .map(_.asScala.toList.map(tinkoffShare => Share(tinkoffShare)))
  }

  override def getCandles(share: Share,
                          from: Instant,
                          to: Instant,
                          candleInterval: CandleInterval): Future[List[Candle]] = {
    marketDataService
      .getCandles(
        share.figi,
        Instant.now().minus(1, ChronoUnit.DAYS),
        Instant.now(),
        candleInterval
      )
      .asScala
      .map(_.asScala.toList.map(historicCandle => Candle(historicCandle, candleInterval)))
  }
}

object InvestApiClientImpl {
  def apply(appConfig: AppConfig): InvestApiClientImpl = new InvestApiClientImpl(appConfig.tinkoffInvestApiToken)
}
