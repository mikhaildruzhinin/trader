package ru.mikhaildruzhinin.trader

import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import org.scalatest.Outcome
import org.scalatest.funsuite.FixtureAnyFunSuite
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import ru.mikhaildruzhinin.trader.core.handlers._
import ru.mikhaildruzhinin.trader.core.services._
import ru.mikhaildruzhinin.trader.core.services.base._
import ru.mikhaildruzhinin.trader.database.connection.Connection
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import java.util.concurrent.TimeUnit
import scala.annotation.tailrec
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class IntegrationSuite extends FixtureAnyFunSuite with Components {

  case class FixtureParam(connection: Connection, sleepMillis: Int)

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
    implicit val connection: Connection = createConnection(config)
    val sleepMillis: Int = 5000

    import connection.databaseConfig.profile.api._

    connection.run(sqlu"create schema trader")
    StartUpHandler()

    try {
      withFixture(test.toNoArgTest(FixtureParam(connection, sleepMillis)))
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

      val shareService: BaseShareService = new ShareService(investApiClient, connection)
      val historicCandleService: BaseHistoricCandleService = new HistoricCandleService(investApiClient, connection)

      val numInsertedShares = for {
        shares <- shareService.getAvailableShares
        candles <- historicCandleService.getWrappedCandles(shares)
        updatedShares <- shareService.getUpdatedShares(shares, candles)
        num <- shareService.persistShares(updatedShares)
      } yield num

      val r = Await.result(numInsertedShares, Duration(10, TimeUnit.SECONDS))
      log.info(s"Total: ${r.getOrElse(0)}")

      val uptrendShares: Int = UptrendHandler()
      val purchasedShares: Int = PurchaseHandler()

      val stopLossSoldShares = monitorShares(3, f.sleepMillis)

      Thread.sleep(f.sleepMillis)
      val closeSoldShares = SellHandler()
    }
  }
}
