package ru.mikhaildruzhinin.trader

import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import org.scalatest.Outcome
import org.scalatest.funsuite.FixtureAnyFunSuite
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import ru.mikhaildruzhinin.trader.core.TypeCode
import ru.mikhaildruzhinin.trader.core.handlers._
import ru.mikhaildruzhinin.trader.core.services.base._
import ru.mikhaildruzhinin.trader.core.services.impl._
import ru.mikhaildruzhinin.trader.database.connection.Connection
import ru.mikhaildruzhinin.trader.database.tables.ShareDAO
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import java.util.concurrent.TimeUnit
import scala.annotation.tailrec
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class IntegrationSuite extends FixtureAnyFunSuite with Components {

  case class FixtureParam(connection: Connection,
                          shareService: BaseShareService,
                          historicCandleService: BaseHistoricCandleService,
                          priceService: BasePriceService,
                          accountService: BaseAccountService,
//                          orderService: BaseOrderService,
                          sleepMillis: Int)

  def updateConfig(port: String): Config = ConfigFactory
    .load()
    .withValue(
      "slick.db.properties.portNumber",
      ConfigValueFactory.fromAnyRef(port)
    )

  def createConnection(config: Config): Connection = new Connection {
    override val databaseConfig: DatabaseConfig[JdbcProfile] = DatabaseConfig
      .forConfig[JdbcProfile]("slick", config)
  }

  override def withFixture(test: OneArgTest): Outcome = {
    val postgresContainer: PostgreSQLContainer[_] = new PostgreSQLContainer(
      DockerImageName.parse("postgres:14.1-alpine"))
    postgresContainer.withUsername(appConfig.slick.db.properties.user)
    postgresContainer.withPassword(appConfig.slick.db.properties.password)
    postgresContainer.withDatabaseName(appConfig.slick.db.properties.databaseName)
    postgresContainer.start()

    val config = updateConfig(postgresContainer.getMappedPort(5432).toString)
    val connection: Connection = createConnection(config)

    val shareDAO: ShareDAO = new ShareDAO(connection.databaseConfig.profile)

    val shareService: BaseShareService = new ShareService(investApiClient, connection, shareDAO)
    val historicCandleService: BaseHistoricCandleService = new HistoricCandleService(investApiClient)
    val priceService: BasePriceService = new PriceService(investApiClient)
    val accountService: BaseAccountService = new AccountService(investApiClient)
//    val orderService: BaseOrderService = new OrderService(investApiClient)

    val sleepMillis: Int = 5000

    import connection.databaseConfig.profile.api._

    connection.run(sqlu"create schema trader")
    shareService.startUp()

    try {
      withFixture(test.toNoArgTest(
        FixtureParam(
          connection,
          shareService,
          historicCandleService,
          priceService,
          accountService,
//          orderService,
          sleepMillis
        )
      ))
    }
    finally {
      postgresContainer.stop()
    }
  }

  def monitorShares(n: Int, sleepMillis: Int)
                   (implicit connection: Connection): Int = {

    @tailrec
    def monitorTailRec(acc: Int, n: Int)
                      (implicit connection: Connection): Int = {

      if (n > 0) {
        Thread.sleep(sleepMillis)
        val soldShares = MonitorHandler()
        monitorTailRec(acc + soldShares, n - 1)
      } else acc
    }

    monitorTailRec(0, n)
  }

  test("test") {
    f => {
      implicit val connection: Connection = f.connection

      val result = for {
        shares <- f.shareService.getAvailableShares
        candles <- f.historicCandleService.getWrappedCandles(shares)
        availableShares <- f.shareService.getUpdatedShares(shares, candles)
        numAvailableShares <- f.shareService.persistNewShares(availableShares, TypeCode.Available)
        _ <- Future { log.info(s"Available: ${numAvailableShares.getOrElse(0)}") }

//        persistedShares <- f.shareService.getPersistedShares(TypeCode.Available)
        currentPrices <- f.priceService.getCurrentPrices(availableShares)
        updatedShares <- f.shareService.updatePrices(availableShares, currentPrices)
        numUpdatedShares <- f.shareService.persistUpdatedShares(updatedShares, TypeCode.Available)
        _ <- Future { log.info(s"Updated: ${numUpdatedShares.sum}") }

        uptrendShares <- f.shareService.filterUptrend(updatedShares)
        numUptrendShares <- f.shareService.persistUpdatedShares(uptrendShares, TypeCode.Uptrend)
        _ <- Future { log.info(s"Best uptrend: ${numUptrendShares.sum}") }

        account <- f.accountService.getAccount
//        sharesForPurchase <- f.shareService.getPersistedShares(TypeCode.Uptrend)
//        orders <- Future.sequence(sharesForPurchase.map(s => f.orderService.buy(s, 1, account)))
        numPurchasedShares <- f.shareService.persistUpdatedShares(uptrendShares, TypeCode.Purchased)
        _ <- Future { log.info(s"Purchased: ${numPurchasedShares.sum}") }
      } yield numPurchasedShares

      Await.result(result, Duration(10, TimeUnit.SECONDS))

//      val purchasedShares: Int = PurchaseHandler()

      val stopLossSoldShares = monitorShares(3, f.sleepMillis)

      Thread.sleep(f.sleepMillis)
      val closeSoldShares = SellHandler()
    }
  }
}
