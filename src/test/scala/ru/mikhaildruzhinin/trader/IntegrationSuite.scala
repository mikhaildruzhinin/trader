package ru.mikhaildruzhinin.trader

import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import ru.mikhaildruzhinin.trader.client.{BaseInvestApiClient, SyncInvestApiClient}
import ru.mikhaildruzhinin.trader.config.{AppConfig, ConfigReader}
import ru.mikhaildruzhinin.trader.core.handlers._
import ru.mikhaildruzhinin.trader.database.connection.{Connection, DatabaseConnection}

class IntegrationSuite extends AnyFunSuite with Matchers with BeforeAndAfterAll {
  implicit val appConfig: AppConfig = ConfigReader.appConfig
  implicit val connection: Connection = DatabaseConnection
  implicit val investApiClient: BaseInvestApiClient = SyncInvestApiClient

  val sleepMillis: Int = 5000

  override def beforeAll(): Unit = StartUpHandler()

//  override def afterAll(): Unit = connection.run(SharesTable.delete())

  test("integration test") {
    AvailabilityHandler()
    val uptrendShares: Int = UptrendHandler()
    val purchasedShares: Int = PurchaseHandler()

    for (_ <- 0 until 3) {
      Thread.sleep(sleepMillis)
      MonitorHandler()
    }
    Thread.sleep(sleepMillis)
    val soldShares = SellHandler()

    soldShares should be >= 0
    soldShares should be <= 10
    soldShares should be <= uptrendShares
    soldShares should be <= purchasedShares
  }


}
