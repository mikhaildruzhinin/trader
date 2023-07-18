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
    UptrendHandler()
    PurchaseHandler()

    for (_ <- 0 until 3) {
      Thread.sleep(sleepMillis)
      MonitorHandler()
    }
    Thread.sleep(sleepMillis)
    val r = SellHandler()

    r should be >= 0
    r should be <= 10
  }


}
