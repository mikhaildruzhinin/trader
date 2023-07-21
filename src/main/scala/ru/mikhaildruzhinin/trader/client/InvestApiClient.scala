package ru.mikhaildruzhinin.trader.client

import com.typesafe.scalalogging.Logger
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.tinkoff.piapi.contract.v1._
import ru.tinkoff.piapi.core.InvestApi
import ru.tinkoff.piapi.core.exception.ApiRuntimeException

import java.time.Instant
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

class InvestApiClient(implicit appConfig: AppConfig,
                      investApi: InvestApi) extends BaseInvestApiClient {

  val log: Logger = Logger(getClass.getName)

  override def getCandles(figi: String,
                          from: Instant,
                          to: Instant,
                          interval: CandleInterval): List[HistoricCandle] = Try {
      investApi.getMarketDataService
        .getCandlesSync(figi, from, to, interval)
        .asScala
        .toList
    } match {
      case Success(candles) => candles
      case Failure(exception: ApiRuntimeException) =>
        log.error(exception.toString)
        Thread.sleep(appConfig.tinkoffInvestApi.rateLimitPauseMillis)
        getCandles(figi, from, to, interval)
      case Failure(exception) =>
        log.error(exception.toString)
        throw exception
    }

  override def getShares: List[Share] = Try {
      investApi.getInstrumentsService
        .getSharesSync(InstrumentStatus.INSTRUMENT_STATUS_BASE)
        .asScala
        .toList
    } match {
      case Success(shares) => shares
      case Failure(exception: ApiRuntimeException) =>
        log.error(exception.toString)
        Thread.sleep(appConfig.tinkoffInvestApi.rateLimitPauseMillis)
        getShares
      case Failure(exception) =>
        log.error(exception.toString)
        throw exception
    }

  override def getLastPrices(figi: Seq[String]): Seq[LastPrice] = Try {
      investApi.getMarketDataService
        .getLastPricesSync(figi.asJava)
        .asScala
        .toList
    } match {
      case Success(lastPrices) => lastPrices
      case Failure(exception: ApiRuntimeException) =>
        log.error(exception.toString)
        Thread.sleep(appConfig.tinkoffInvestApi.rateLimitPauseMillis)
        getLastPrices(figi)
      case Failure(exception) =>
        log.error(exception.toString)
        throw exception
    }
}

object InvestApiClient {
  def apply()(implicit appConfig: AppConfig,
            investApi: InvestApi): InvestApiClient = new InvestApiClient()
}
