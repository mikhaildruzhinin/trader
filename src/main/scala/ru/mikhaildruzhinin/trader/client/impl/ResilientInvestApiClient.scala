package ru.mikhaildruzhinin.trader.client.impl

import io.github.resilience4j.ratelimiter.{RateLimiter, RateLimiterConfig, RateLimiterRegistry}
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.tinkoff.piapi.contract.v1.{Account, CandleInterval, HistoricCandle, LastPrice, OrderDirection, OrderType, PostOrderResponse, Quotation, Share}
import ru.tinkoff.piapi.core.InvestApi

import java.time.temporal.ChronoUnit
import java.time.{Duration, Instant}
import java.util.UUID
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

  //noinspection ScalaWeakerAccess
  protected def limit[T](rateLimiter: RateLimiter,
                         callable: Callable[T]): T = RateLimiter
    .decorateCallable(rateLimiter, callable)
    .call()

  //noinspection ScalaWeakerAccess
  final protected def retry[T](numAttempts: Int)(fn: => Future[T]): Future[T] = {
    fn.transformWith {
      case Success(x) => Future { x }
      case Failure(exception) if numAttempts > 1 =>
        log.error(s"Tinkoff invest API error\n\t${exception.toString}\n\tattempts left: ${numAttempts - 1}")
        Thread.sleep(appConfig.tinkoffInvestApi.retry.pauseMillis)
        retry(numAttempts - 1)(fn)
      case Failure(exception) =>
        log.error(s"Tinkoff invest API error\n\t${exception.toString}\n\tattempts left: ${numAttempts - 1}")
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
    callable = () => super.getCandles(figi, from, to, interval)
  ))

  override def getShares: Future[Seq[Share]] = retry(
    appConfig.tinkoffInvestApi.retry.numAttempts
  )(limit(
    rateLimiter = instrumentsRateLimiter,
    callable = () => super.getShares
  ))

  override def getLastPrices(figi: Seq[String]): Future[Seq[LastPrice]] = retry(
    appConfig.tinkoffInvestApi.retry.numAttempts
  )(limit(
    rateLimiter = marketDataRateLimiter,
    callable = () => super.getLastPrices(figi)
  ))

  override def getAccount: Future[Account] = retry(
    appConfig.tinkoffInvestApi.retry.numAttempts
  )(limit(
    rateLimiter = usersRateLimiter,
    callable = () => super.getAccount
  ))

  override def postOrder(figi: String,
                         quantity: Long,
                         price: Quotation,
                         direction: OrderDirection,
                         accountId: String,
                         orderType: OrderType,
                         orderId: UUID): Future[PostOrderResponse] = retry(
    appConfig.tinkoffInvestApi.retry.numAttempts
  )(limit(
    rateLimiter = ordersRateLimiter,
    callable = () => super.postOrder(
      figi,
      quantity,
      price,
      direction,
      accountId,
      orderType,
      orderId
    )
  ))
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
