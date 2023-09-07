package ru.mikhaildruzhinin.trader

import org.scalatest.GivenWhenThen

import java.util.concurrent.TimeUnit
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class IntegrationSpec extends BaseIntegrationSpec with GivenWhenThen {
  Feature("Trader integration with Tinkoff invest API and database") {
    Scenario("End-to-end testing") {
      f =>
        Given("a set of handler functions for purchasing, monitoring and selling shares")

        When("handlers are processing shares")
        val (purchasedSharesNum, stopLossSoldSharesNum, soldSharesNum) = Await.result(
          awaitable = for {
            purchasedSharesNum <- f.shareService.purchaseUptrendShares()
            stopLossSoldSharesNum <- f.shareService.monitorPurchasedShares()
            soldSharesNum <- f.shareService.sellShares()
          } yield (purchasedSharesNum, stopLossSoldSharesNum, soldSharesNum),
          atMost = Duration(30, TimeUnit.SECONDS)
        )

        Then("the number of purchased shares should be equal to the number of sold shares")
        info(s"Purchased: $purchasedSharesNum")
        info(s"Stop loss sold: $stopLossSoldSharesNum")
        info(s"Sold: $soldSharesNum")
        assert(purchasedSharesNum == stopLossSoldSharesNum + soldSharesNum)
    }
  }
}

