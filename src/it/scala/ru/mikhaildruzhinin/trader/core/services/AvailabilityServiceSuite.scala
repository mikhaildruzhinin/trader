package ru.mikhaildruzhinin.trader.core.services

import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import org.scalatest.Outcome
import org.scalatest.funsuite.FixtureAnyFunSuite
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import ru.mikhaildruzhinin.trader.Components
import ru.mikhaildruzhinin.trader.core.handlers.StartUpHandler
import ru.mikhaildruzhinin.trader.database.connection.Connection
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import java.util.concurrent.TimeUnit
import scala.concurrent.Await
import scala.concurrent.duration.Duration

class AvailabilityServiceSuite extends FixtureAnyFunSuite with Components {
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

  test("test availability service") {
    f => {
      implicit val connection: Connection = f.connection

      val service = new AvailabilityService(investApiClient, connection)
      val r = Await.result(service.getAvailableShares, Duration(10, TimeUnit.SECONDS))
      println(r)
    }
  }
}
