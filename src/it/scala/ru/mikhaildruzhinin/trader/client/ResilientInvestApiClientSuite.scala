package ru.mikhaildruzhinin.trader.client

import com.typesafe.scalalogging.Logger
import org.scalatest.Outcome
import org.scalatest.funsuite.FixtureAnyFunSuite
import pureconfig.generic.ProductHint
import pureconfig.generic.auto.exportReader
import pureconfig.generic.semiauto.deriveEnumerationReader
import pureconfig.{CamelCase, ConfigFieldMapping, ConfigReader, ConfigSource}
import ru.mikhaildruzhinin.trader.client.base.BaseInvestApiClient
import ru.mikhaildruzhinin.trader.client.impl.ResilientInvestApiClient
import ru.mikhaildruzhinin.trader.config.{AppConfig, InvestApiMode}
import ru.mikhaildruzhinin.trader.core.services.base._
import ru.mikhaildruzhinin.trader.core.services.impl._
import ru.mikhaildruzhinin.trader.database.{Connection, DatabaseConnection}
import ru.mikhaildruzhinin.trader.database.tables.ShareDAO
import ru.tinkoff.piapi.core.InvestApi

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

class ResilientInvestApiClientSuite extends FixtureAnyFunSuite {
  case class FixtureParam(investApiClient: BaseInvestApiClient)

  override protected def withFixture(test: OneArgTest): Outcome = {
    val log: Logger = Logger(getClass.getName)

    implicit def hint[A]: ProductHint[A] = ProductHint[A](ConfigFieldMapping(CamelCase, CamelCase))

    implicit lazy val investApiModeConvert: ConfigReader[InvestApiMode] = deriveEnumerationReader[InvestApiMode]
    implicit lazy val appConfig: AppConfig = ConfigSource.default.loadOrThrow[AppConfig]

    lazy val investApi: InvestApi = appConfig.tinkoffInvestApi.mode match {
      case InvestApiMode.Trade => InvestApi.create(appConfig.tinkoffInvestApi.token)
      case InvestApiMode.Readonly => InvestApi.createReadonly(appConfig.tinkoffInvestApi.token)
      case InvestApiMode.Sandbox => InvestApi.createSandbox(appConfig.tinkoffInvestApi.token)
    }

    implicit lazy val investApiClient: BaseInvestApiClient = ResilientInvestApiClient(investApi)

    implicit lazy val connection: Connection = DatabaseConnection

    val shareDAO: ShareDAO = new ShareDAO(connection.databaseConfig.profile)

    val shareService: BaseShareService = new ShareService(investApiClient, connection, shareDAO)
    val historicCandleService: BaseHistoricCandleService = new HistoricCandleService(investApiClient)

    withFixture(test.toNoArgTest(FixtureParam(investApiClient)))
  }

  test("testGetShares") {
    f => {

      (0 to 1000).foreach(i => {
        val r = f.investApiClient.getShares

        r onComplete {
          case Success(value) => println(i, value.length)
          case Failure(exception) => println(i, exception.toString)
        }
      })
    }
  }
}
