package ru.mikhaildruzhinin.trader.client

import com.typesafe.scalalogging.Logger
import org.scalatest.Outcome
import org.scalatest.funsuite.FixtureAnyFunSuite
import pureconfig.generic.ProductHint
import pureconfig.generic.auto.exportReader
import pureconfig.generic.semiauto.deriveEnumerationReader
import pureconfig.{CamelCase, ConfigFieldMapping, ConfigReader, ConfigSource}
import ru.mikhaildruzhinin.trader.client.impl.ResilientInvestApiClientImpl
import ru.mikhaildruzhinin.trader.config.{AppConfig, InvestApiMode}
import ru.mikhaildruzhinin.trader.database.tables.ShareDAO
import ru.mikhaildruzhinin.trader.database.tables.impl.ShareDAOImpl
import ru.mikhaildruzhinin.trader.database.{Connection, ConnectionImpl}
import ru.mikhaildruzhinin.trader.services._
import ru.mikhaildruzhinin.trader.services.impl._
import ru.tinkoff.piapi.core.InvestApi

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

class ResilientInvestApiClientSuite extends FixtureAnyFunSuite {
  case class FixtureParam(investApiClient: InvestApiClient)

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

    implicit lazy val investApiClient: InvestApiClient = ResilientInvestApiClientImpl(investApi)

    implicit lazy val connection: Connection = ConnectionImpl

    val shareDAO: ShareDAO = new ShareDAOImpl(connection)

    val historicCandleService: CandleService = new CandleServiceImpl(investApiClient)
    val priceService: PriceService = new PriceServiceImpl(investApiClient)
    val accountService: AccountService = new AccountServiceImpl(investApiClient)
    val shareService: ShareService = new ShareServiceImpl(investApiClient, connection, shareDAO, historicCandleService, priceService, accountService)


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
