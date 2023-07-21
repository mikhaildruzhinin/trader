package ru.mikhaildruzhinin.trader

import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import ru.mikhaildruzhinin.trader.core.handlers._

import scala.annotation.tailrec

class IntegrationSuite extends AnyFunSuite
  with Matchers with BeforeAndAfterAll with Components {

  val sleepMillis: Int = 5000

  override def beforeAll(): Unit = StartUpHandler()

//  override def afterAll(): Unit = connection.run(SharesTable.delete())

  def monitorShares(n: Int): Int = {
    @tailrec
    def monitorTailRec(acc: Int, n: Int): Int = {
      if (n > 0) {
        Thread.sleep(sleepMillis)
        val soldShares = MonitorHandler()
        monitorTailRec(acc + soldShares, n - 1)
      } else acc
    }
    monitorTailRec(0, n)
  }

  test("integration test") {
    AvailabilityHandler()
    val uptrendShares: Int = UptrendHandler()
    val purchasedShares: Int = PurchaseHandler()

    val stopLossSoldShares = monitorShares(3)

    Thread.sleep(sleepMillis)
    val closeSoldShares = SellHandler()

    uptrendShares should be (purchasedShares)
    purchasedShares should be (stopLossSoldShares + closeSoldShares)
    closeSoldShares should be >= 0
    closeSoldShares should be <= 10
  }
}
