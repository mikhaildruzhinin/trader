package ru.mikhaildruzhinin.trader.client

import io.github.resilience4j.ratelimiter.{RateLimiter, RateLimiterConfig, RateLimiterRegistry}
import io.github.resilience4j.retry.{Retry, RetryConfig, RetryRegistry}
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.tinkoff.piapi.core.exception.ApiRuntimeException

import java.time.Duration
import java.time.temporal.ChronoUnit

class FaultTolerance(implicit appConfig: AppConfig) {
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

  private def getRateLimiterConfig(period: Int,
                                   timeUnit: ChronoUnit,
                                   limit: Int) = RateLimiterConfig
    .custom()
    .limitRefreshPeriod(Duration.of(period, timeUnit))
    .limitForPeriod(limit)
    .timeoutDuration(Duration.of(period, timeUnit))
    .build()

  private def getRateLimiter(limit: Int, name: String) = RateLimiterRegistry.of(
    getRateLimiterConfig(
      period = appConfig.tinkoffInvestApi.limits.period,
      timeUnit = appConfig.tinkoffInvestApi.limits.timeUnit,
      limit = limit
    )
  ).rateLimiter(name)
}
