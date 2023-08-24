package ru.mikhaildruzhinin.trader

import ru.mikhaildruzhinin.trader.client.base.BaseInvestApiClient
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.mikhaildruzhinin.trader.core.handlers._
import ru.mikhaildruzhinin.trader.core.{Monitor, Purchase}
import ru.mikhaildruzhinin.trader.database.Connection

import java.util.concurrent.TimeUnit
import scala.annotation.tailrec
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class IntegrationSuite extends BaseIntegrationSuite {

  def monitorShares(n: Int, sleepMillis: Int)
                   (implicit appConfig: AppConfig,
                    investApiClient: BaseInvestApiClient,
                    connection: Connection): Int = {

    @tailrec
    def monitorTailRec(acc: Int, n: Int)
                      (implicit appConfig: AppConfig,
                       investApiClient: BaseInvestApiClient,
                       connection: Connection): Int = {

      if (n > 0) {
        Thread.sleep(sleepMillis)
//        val soldShares = MonitorHandler()
        monitorTailRec(acc /*+ soldShares*/, n - 1)
      } else acc
    }

    monitorTailRec(0, n)
  }

  test("test") {
    f => {
      implicit val appConfig: AppConfig = f.appConfig
      implicit val investApiClient: BaseInvestApiClient = f.investApiClient
      implicit val connection: Connection = f.connection

      val result = for {
        _ <- Purchase(f.services)
        _ <- Monitor(f.services)
      } yield ()

      Await.result(result, Duration(15, TimeUnit.SECONDS))

//      val stopLossSoldShares = monitorShares(3, f.sleepMillis)

      Thread.sleep(f.sleepMillis)
      val closeSoldShares = SellHandler()
    }
  }
}
