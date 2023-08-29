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
import ru.mikhaildruzhinin.trader.core.services.base._
import ru.mikhaildruzhinin.trader.core.services.impl._
import ru.mikhaildruzhinin.trader.database.Connection
import ru.mikhaildruzhinin.trader.database.tables.ShareDAO
import ru.tinkoff.piapi.core.InvestApi

abstract class BaseIntegrationSuite extends FixtureAnyFunSuite {

  val log: Logger = Logger(getClass.getName)

  case class FixtureParam(appConfig: AppConfig,
                          investApiClient: BaseInvestApiClient,
                          connection: Connection,
                          services: Services)

  def updateConfig(port: String): Config = ConfigFactory
    .load()
    .withValue(
      "slick.db.properties.portNumber",
      ConfigValueFactory.fromAnyRef(port)
    )

  override def withFixture(test: OneArgTest): Outcome = {
    import com.softwaremill.macwire.wire

    implicit def hint[A]: ProductHint[A] = ProductHint[A](ConfigFieldMapping(CamelCase, CamelCase))
    implicit lazy val investApiModeConvert: ConfigReader[InvestApiMode] = deriveEnumerationReader[InvestApiMode]
    implicit lazy val appConfig: AppConfig = ConfigSource.default.loadOrThrow[AppConfig]

    val postgresContainer: PostgreSQLContainer[_] = new PostgreSQLContainer(
      DockerImageName.parse("postgres:14.1-alpine")
    )
    postgresContainer.withUsername(appConfig.slick.db.properties.user)
    postgresContainer.withPassword(appConfig.slick.db.properties.password)
    postgresContainer.withDatabaseName(appConfig.slick.db.properties.databaseName)
    postgresContainer.start()

    val port: Int = postgresContainer.getMappedPort(appConfig.slick.db.properties.portNumber)

    lazy val investApi: InvestApi = appConfig.tinkoffInvestApi.mode match {
      case InvestApiMode.Trade => InvestApi.create(appConfig.tinkoffInvestApi.token)
      case InvestApiMode.Readonly => InvestApi.createReadonly(appConfig.tinkoffInvestApi.token)
      case InvestApiMode.Sandbox => InvestApi.createSandbox(appConfig.tinkoffInvestApi.token)
    }

    lazy val investApiClient: BaseInvestApiClient = wire[ResilientInvestApiClient]
    lazy val config = updateConfig(port.toString)
    lazy val connection: Connection = Connection("slick", config)
    lazy val shareDAO: ShareDAO = wire[ShareDAO]
    lazy val shareService: BaseShareService = wire[ShareService]
    lazy val historicCandleService: BaseHistoricCandleService = wire[HistoricCandleService]
    lazy val priceService: BasePriceService = wire[PriceService]
    lazy val accountService: BaseAccountService = wire[AccountService]
    lazy val services: Services = wire[Services]
    lazy val fixtureParam: FixtureParam = wire[FixtureParam]

    val dataSource: PGSimpleDataSource = appConfig.slick.db.properties.dataSource
    dataSource.setPortNumbers(Array(port))

    Flyway.configure()
      .dataSource(dataSource)
      .schemas("trader")
      .load()
      .migrate()

    try {
      withFixture(test.toNoArgTest(fixtureParam))
    }
    finally {
      postgresContainer.stop()
    }
  }
}
