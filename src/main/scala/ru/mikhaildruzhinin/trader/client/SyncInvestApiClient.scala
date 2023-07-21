package ru.mikhaildruzhinin.trader.client

import com.typesafe.scalalogging.Logger
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.tinkoff.piapi.contract.v1._
import ru.tinkoff.piapi.core.InvestApi
import ru.tinkoff.piapi.core.exception.ApiRuntimeException

import java.time.Instant
import scala.annotation.tailrec
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

object SyncInvestApiClient extends BaseInvestApiClient {
  val log: Logger = Logger(getClass.getName)

  @tailrec
  override def getCandles(figi: String,
                 from: Instant,
                 to: Instant,
                 interval: CandleInterval)
                (implicit appConfig: AppConfig,
                 investApi: InvestApi): List[HistoricCandle] = {

    Try {
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
  }

  @tailrec
  override def getShares(implicit appConfig: AppConfig,
                         investApi: InvestApi): List[Share] = {

    Try {
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
  }

  @tailrec
  override def getLastPrices(figi: Seq[String])
                            (implicit appConfig: AppConfig,
                             investApi: InvestApi): Seq[LastPrice] = {

    Try {
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
}
