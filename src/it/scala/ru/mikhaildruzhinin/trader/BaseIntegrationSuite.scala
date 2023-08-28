package ru.mikhaildruzhinin.trader

import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import com.typesafe.scalalogging.Logger
import org.flywaydb.core.Flyway
import org.postgresql.ds.PGSimpleDataSource
import org.scalatest.Outcome
import org.scalatest.funsuite.FixtureAnyFunSuite
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import pureconfig.generic.ProductHint
import pureconfig.generic.auto.exportReader
import pureconfig.generic.semiauto.deriveEnumerationReader
import pureconfig.{CamelCase, ConfigFieldMapping, ConfigReader, ConfigSource}
import ru.mikhaildruzhinin.trader.client.base.BaseInvestApiClient
import ru.mikhaildruzhinin.trader.client.impl.ResilientInvestApiClient
import ru.mikhaildruzhinin.trader.config.{AppConfig, InvestApiMode}
import ru.mikhaildruzhinin.trader.core.services.Services
import ru.mikhaildruzhinin.trader.database.Connection
import ru.mikhaildruzhinin.trader.database.tables.ShareDAO
import ru.tinkoff.piapi.core.InvestApi

abstract class BaseIntegrationSuite extends FixtureAnyFunSuite {

  val log: Logger = Logger(getClass.getName)

  case class FixtureParam(appConfig: AppConfig,
                          investApiClient: BaseInvestApiClient,
                          connection: Connection,
                          services: Services,
                          sleepMillis: Int)

  def updateConfig(port: String): Config = ConfigFactory
    .load()
    .withValue(
      "slick.db.properties.portNumber",
      ConfigValueFactory.fromAnyRef(port)
    )

  override def withFixture(test: OneArgTest): Outcome = {
    implicit def hint[A]: ProductHint[A] = ProductHint[A](ConfigFieldMapping(CamelCase, CamelCase))
    implicit lazy val investApiModeConvert: ConfigReader[InvestApiMode] = deriveEnumerationReader[InvestApiMode]
    implicit lazy val appConfig: AppConfig = ConfigSource.default.loadOrThrow[AppConfig]

    lazy val investApi: InvestApi = appConfig.tinkoffInvestApi.mode match {
      case InvestApiMode.Trade => InvestApi.create(appConfig.tinkoffInvestApi.token)
      case InvestApiMode.Readonly => InvestApi.createReadonly(appConfig.tinkoffInvestApi.token)
      case InvestApiMode.Sandbox => InvestApi.createSandbox(appConfig.tinkoffInvestApi.token)
    }

    implicit lazy val investApiClient: BaseInvestApiClient = ResilientInvestApiClient(investApi)

    val postgresContainer: PostgreSQLContainer[_] = new PostgreSQLContainer(
      DockerImageName.parse("postgres:14.1-alpine"))
    postgresContainer.withUsername(appConfig.slick.db.properties.user)
    postgresContainer.withPassword(appConfig.slick.db.properties.password)
    postgresContainer.withDatabaseName(appConfig.slick.db.properties.databaseName)
    postgresContainer.start()

    val port: Int = postgresContainer.getMappedPort(appConfig.slick.db.properties.portNumber)

    val config = updateConfig(port.toString)
    val connection: Connection = Connection("slick", config)
    val shareDAO: ShareDAO = new ShareDAO(connection.databaseConfig.profile)
    val services = Services(investApiClient, connection, shareDAO)
    val sleepMillis: Int = 5000

    val dataSource: PGSimpleDataSource = appConfig.slick.db.properties.dataSource
    dataSource.setPortNumbers(Array(port))

    Flyway.configure()
      .dataSource(dataSource)
      .schemas("trader")
      .load()
      .migrate()

    try {
      withFixture(test.toNoArgTest(
        FixtureParam(
          appConfig,
          investApiClient,
          connection,
          services,
          sleepMillis
        )
      ))
    }
    finally {
      postgresContainer.stop()
    }
  }
}
