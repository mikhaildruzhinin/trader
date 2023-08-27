package ru.mikhaildruzhinin.trader

import ru.mikhaildruzhinin.trader.core.{Monitor, Purchase, Sell}

import java.util.concurrent.TimeUnit
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class IntegrationSuite extends BaseIntegrationSuite {
  test("test") {
    f => {
      Await.result(
        awaitable = for {
          _ <- Purchase(f.services)
          _ <- Monitor(f.services)
          _ <- Sell(f.services)
        } yield (),
        atMost = Duration(15, TimeUnit.SECONDS)
      )
    }
  }
}
