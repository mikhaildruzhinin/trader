package ru.mikhaildruzhinin.trader.client

import io.github.resilience4j.ratelimiter.{RateLimiter, RateLimiterConfig, RateLimiterRegistry}
import io.github.resilience4j.retry.{MaxRetriesExceeded, Retry, RetryConfig, RetryRegistry}
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.tinkoff.piapi.contract.v1._
import ru.tinkoff.piapi.core.InvestApi
import ru.tinkoff.piapi.core.exception.ApiRuntimeException

import java.time.temporal.ChronoUnit
import java.time.{Duration, Instant}
import java.util.concurrent.Callable
import scala.util.{Failure, Success, Try}

class ResilientInvestApiClient private (investApi: InvestApi,
                                        instrumentsRateLimiter: RateLimiter,
                                        marketDataRateLimiter: RateLimiter,
                                        ordersRateLimiter: RateLimiter,
                                        usersRateLimiter: RateLimiter,
                                        retry: Retry)
                                       (implicit appConfig: AppConfig)
  extends InvestApiClient(investApi) {

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
    rateLimiter = marketDataRateLimiter,
    retry = retry,
    callable = () => super.getCandles(figi, from, to, interval),
    resultOnFailure = List(HistoricCandle.newBuilder().build())
  )

  override def getShares: List[Share] = decorateRequest(
    rateLimiter = instrumentsRateLimiter,
    retry = retry,
    callable = () => super.getShares,
    resultOnFailure = List(Share.newBuilder.build())
  )

  override def getLastPrices(figi: Seq[String]): Seq[LastPrice] = decorateRequest(
    rateLimiter = marketDataRateLimiter,
    retry = retry,
    callable = () => super.getLastPrices(figi),
    resultOnFailure = List(LastPrice.newBuilder.build())
  )
}

object ResilientInvestApiClient {
  private def getRateLimiterConfig(period: Int,
                                   timeUnit: ChronoUnit,
                                   limit: Int)
                                  (implicit appConfig: AppConfig) = RateLimiterConfig
    .custom()
    .limitRefreshPeriod(Duration.of(period, timeUnit))
    .limitForPeriod(limit)
    .timeoutDuration(Duration.of(period, timeUnit))
    .build()

  private def getRateLimiter(limit: Int, name: String)
                            (implicit appConfig: AppConfig) = RateLimiterRegistry.of(
    getRateLimiterConfig(
      period = appConfig.tinkoffInvestApi.limits.period,
      timeUnit = appConfig.tinkoffInvestApi.limits.timeUnit,
      limit = limit
    )
  ).rateLimiter(name)

  def apply(investApi: InvestApi)
           (implicit appConfig: AppConfig): ResilientInvestApiClient = {

    lazy val instrumentsRateLimiter: RateLimiter = getRateLimiter(
      limit = appConfig.tinkoffInvestApi.limits.services.instruments,
      name = "instrumentsRateLimiter"
    )

    lazy val marketDataRateLimiter: RateLimiter = getRateLimiter(
      limit = appConfig.tinkoffInvestApi.limits.services.marketData,
      name = "marketDataRateLimiter"
    )

    lazy val ordersRateLimiter: RateLimiter = getRateLimiter(
      limit = appConfig.tinkoffInvestApi.limits.services.orders,
      name = "ordersRateLimiter"
    )

    lazy val usersRateLimiter: RateLimiter = getRateLimiter(
      limit = appConfig.tinkoffInvestApi.limits.services.users,
      name = "usersRateLimiter"
    )

    lazy val retry: Retry = RetryRegistry.of(
      RetryConfig.custom()
        .maxAttempts(3)
        .waitDuration(Duration.of(1, ChronoUnit.MINUTES))
        .retryOnException(_.isInstanceOf[ApiRuntimeException])
        .failAfterMaxAttempts(true)
        .build()
    ).retry("retry")

    new ResilientInvestApiClient(
      investApi,
      instrumentsRateLimiter,
      marketDataRateLimiter,
      ordersRateLimiter,
      usersRateLimiter,
      retry
    )
  }


}
