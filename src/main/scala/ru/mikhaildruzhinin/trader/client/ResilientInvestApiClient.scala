package ru.mikhaildruzhinin.trader.client

import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.retry.{MaxRetriesExceeded, Retry}
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.tinkoff.piapi.contract.v1._
import ru.tinkoff.piapi.core.InvestApi
import ru.tinkoff.piapi.core.exception.ApiRuntimeException

import java.time.Instant
import java.util.concurrent.Callable
import scala.util.{Failure, Success, Try}

class ResilientInvestApiClient(investApi: InvestApi,
                               faultTolerance: FaultTolerance)
                              (implicit appConfig: AppConfig) extends InvestApiClient(investApi) {

   private def decorateRequest[T](rateLimiter: RateLimiter,
                                  retry: Retry,
                                  callable: Callable[T],
                                  resultOnFailure: T): T = Try {
    Retry.decorateCallable(
      retry,
      RateLimiter.decorateCallable(rateLimiter, callable)
    ).call() } match {
      case Success(response) => response
      case Failure(exception: ApiRuntimeException) =>
        log.error(exception.toString)
        resultOnFailure
      case Failure(exception: MaxRetriesExceeded) =>
        log.error(exception.toString)
        resultOnFailure
      case Failure(exception) => throw exception
    }

  override def getCandles(figi: String,
                          from: Instant,
                          to: Instant,
                          interval: CandleInterval): List[HistoricCandle] = decorateRequest(
    rateLimiter = faultTolerance.marketDataRateLimiter,
    retry = faultTolerance.retry,
    callable = () => super.getCandles(figi, from, to, interval),
    resultOnFailure = List(HistoricCandle.newBuilder().build())
  )

  override def getShares: List[Share] = decorateRequest(
    rateLimiter = faultTolerance.instrumentsRateLimiter,
    retry = faultTolerance.retry,
    callable = () => super.getShares,
    resultOnFailure = List(Share.newBuilder.build())
  )

  override def getLastPrices(figi: Seq[String]): Seq[LastPrice] = decorateRequest(
    rateLimiter = faultTolerance.marketDataRateLimiter,
    retry = faultTolerance.retry,
    callable = () => super.getLastPrices(figi),
    resultOnFailure = List(LastPrice.newBuilder.build())
  )
}
