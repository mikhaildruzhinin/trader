package ru.mikhaildruzhinin.trader.core.services

import org.scalamock.scalatest.MockFactory
import org.scalatest.Outcome
import org.scalatest.funsuite.FixtureAnyFunSuite
import pureconfig.generic.ProductHint
import pureconfig.generic.auto.exportReader
import pureconfig.generic.semiauto.deriveEnumerationReader
import pureconfig.{CamelCase, ConfigFieldMapping, ConfigReader, ConfigSource}
import ru.mikhaildruzhinin.trader.client.base.BaseInvestApiClient
import ru.mikhaildruzhinin.trader.config.{AppConfig, InvestApiMode}
import ru.mikhaildruzhinin.trader.core.dto.ShareDTO
import ru.mikhaildruzhinin.trader.core.services.base._
import ru.mikhaildruzhinin.trader.core.services.impl._
import ru.mikhaildruzhinin.trader.database.Connection
import ru.mikhaildruzhinin.trader.database.tables.base.BaseShareDAO
import ru.tinkoff.piapi.core.utils.MapperUtils.bigDecimalToQuotation

import java.util.concurrent.TimeUnit
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.Random

class ShareServiceSuite extends FixtureAnyFunSuite with MockFactory {

  case class FixtureParam(appConfig: AppConfig,
                          investApiClient: BaseInvestApiClient,
                          connection: Connection,
                          shareDAO: BaseShareDAO,
                          historicCandleService: BaseHistoricCandleService,
                          priceService: BasePriceService,
                          accountService: BaseAccountService)

  override def withFixture(test: OneArgTest): Outcome = {
    import com.softwaremill.macwire.wire

    implicit def hint[A]: ProductHint[A] = ProductHint[A](ConfigFieldMapping(CamelCase, CamelCase))

    implicit lazy val investApiModeConvert: ConfigReader[InvestApiMode] = deriveEnumerationReader[InvestApiMode]
    implicit lazy val appConfig: AppConfig = ConfigSource.default.loadOrThrow[AppConfig]

    lazy val investApiClient: BaseInvestApiClient = mock[BaseInvestApiClient]
    lazy val connection: Connection = mock[Connection]
    lazy val shareDAO: BaseShareDAO = mock[BaseShareDAO]
    lazy val historicCandleService: BaseHistoricCandleService = wire[HistoricCandleService]
    lazy val priceService: BasePriceService = wire[PriceService]
    lazy val accountService: BaseAccountService = wire[AccountService]
    lazy val shareService: BaseShareService = wire[ShareService]
    lazy val fixtureParam: FixtureParam = wire[FixtureParam]

    withFixture(test.toNoArgTest(fixtureParam))
  }

  def generateFakeShareDTO(figi: String,
                           purchasePrice: Int,
                           currentPrice: Int)
                          (implicit appConfig: AppConfig): ShareDTO = ShareDTO(

    figi = figi,
    lot = 1,
    quantity = Some(1),
    currency = Random.alphanumeric.take(3).mkString,
    name = Random.alphanumeric.take(12).mkString,
    exchange = Random.alphanumeric.take(4).mkString,
    startingPrice = None,
    purchasePrice = Some(bigDecimalToQuotation(BigDecimal(purchasePrice).bigDecimal)),
    currentPrice = Some(bigDecimalToQuotation(BigDecimal(currentPrice).bigDecimal)),
    updateTime = None
  )

  test("test partitionEnrichedSharesShares") {
    f => {

      implicit val appConfig: AppConfig = f.appConfig

      val shareService = new ShareService(
        f.investApiClient,
        f.connection,
        f.shareDAO,
        f.historicCandleService,
        f.priceService,
        f.accountService
      )

      val shares: Seq[ShareDTO] = Seq(
        ("keep1", 18, 20),
        ("keep2", 20, 18),
        ("sell1", 20, 18),
        ("sell2", 10, 20)
      ).map(s => generateFakeShareDTO(s._1, s._2, s._3))

      val oldRois: Seq[Some[BigDecimal]] = Seq(5, -10, -5, 50)
        .map(r => Some(BigDecimal(r).bigDecimal))

      val (sell, keep) = Await.result(
        shareService.partitionEnrichedSharesShares(shares.zip(oldRois)),
        Duration(10, TimeUnit.SECONDS)
      )

      shares.foreach(println)

      assert(keep.map(_._1.figi) == Seq("keep1", "keep2"))
    }
  }
}
