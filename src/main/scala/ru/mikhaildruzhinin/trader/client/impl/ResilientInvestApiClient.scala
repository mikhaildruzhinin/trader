package ru.mikhaildruzhinin.trader.client.impl

import io.github.resilience4j.ratelimiter.{RateLimiter, RateLimiterConfig, RateLimiterRegistry}
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.tinkoff.piapi.contract.v1.{CandleInterval, HistoricCandle, LastPrice, Share}
import ru.tinkoff.piapi.core.InvestApi

import java.time.temporal.ChronoUnit
import java.time.{Duration, Instant}
import java.util.concurrent.Callable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

class ResilientInvestApiClient private (investApi: InvestApi,
                                        instrumentsRateLimiter: RateLimiter,
                                        marketDataRateLimiter: RateLimiter,
                                        ordersRateLimiter: RateLimiter,
                                        usersRateLimiter: RateLimiter)
                                       (implicit appConfig: AppConfig)
  extends InvestApiClient(investApi) {

  protected def limit[T](rateLimiter: RateLimiter,
                       callable: Callable[T],
                       resultOnFailure: T): T = RateLimiter
    .decorateCallable(rateLimiter, callable)
    .call()

  final protected def retry[T](numAttempts: Int)(fn: => Future[T]): Future[T] = {
    fn.transformWith {
      case Success(x) => Future { x }
      case Failure(exception) if numAttempts > 1 =>
        log.error(s"Tinkoff invest API error\n\t${exception.toString}\n\tattempts left: $numAttempts")
        Thread.sleep(appConfig.tinkoffInvestApi.retry.pauseMillis)
        retry(numAttempts - 1)(fn)
      case Failure(exception) =>
        log.error(s"Tinkoff invest API error\n\t${exception.toString}\n\tattempts left: $numAttempts")
        throw exception
    }
  }

  override def getCandles(figi: String,
                          from: Instant,
                          to: Instant,
                          interval: CandleInterval): Future[Seq[HistoricCandle]] = retry(
    appConfig.tinkoffInvestApi.retry.numAttempts
  )(limit(
    rateLimiter = marketDataRateLimiter,
    callable = () => super.getCandles(figi, from, to, interval),
    resultOnFailure = Future(Seq(HistoricCandle.newBuilder().build()))
  ))

  override def getShares: Future[Seq[Share]] = retry(
    appConfig.tinkoffInvestApi.retry.numAttempts
  )(limit(
    rateLimiter = instrumentsRateLimiter,
    callable = () => super.getShares,
    resultOnFailure = Future(Seq(Share.newBuilder.build()))
  ))

  override def getLastPrices(figi: Seq[String]): Future[Seq[LastPrice]] = limit(
    rateLimiter = marketDataRateLimiter,
    callable = () => super.getLastPrices(figi),
    resultOnFailure = Future(List(LastPrice.newBuilder.build()))
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

    new ResilientInvestApiClient(
      investApi,
      instrumentsRateLimiter,
      marketDataRateLimiter,
      ordersRateLimiter,
      usersRateLimiter
    )
  }
}
