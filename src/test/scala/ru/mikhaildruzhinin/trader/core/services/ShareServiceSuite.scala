package ru.mikhaildruzhinin.trader.core.services

import org.scalamock.scalatest.MockFactory
import org.scalatest.Outcome
import org.scalatest.funsuite.FixtureAnyFunSuite
import pureconfig.generic.ProductHint
import pureconfig.generic.auto.exportReader
import pureconfig.generic.semiauto.deriveEnumerationReader
import pureconfig.{CamelCase, ConfigFieldMapping, ConfigReader, ConfigSource}
import ru.mikhaildruzhinin.trader.client.InvestApiClient
import ru.mikhaildruzhinin.trader.config.{AppConfig, InvestApiMode}
import ru.mikhaildruzhinin.trader.database.Connection
import ru.mikhaildruzhinin.trader.database.tables.ShareDAO
import ru.mikhaildruzhinin.trader.models.ShareModel
import ru.mikhaildruzhinin.trader.services._
import ru.mikhaildruzhinin.trader.services.impl._
import ru.tinkoff.piapi.core.utils.MapperUtils.bigDecimalToQuotation

import java.util.concurrent.TimeUnit
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.Random

class ShareServiceSuite extends FixtureAnyFunSuite with MockFactory {

  case class FixtureParam(appConfig: AppConfig,
                          investApiClient: InvestApiClient,
                          connection: Connection,
                          shareDAO: ShareDAO,
                          historicCandleService: CandleService,
                          priceService: PriceService,
                          accountService: AccountService)

  override def withFixture(test: OneArgTest): Outcome = {
    import com.softwaremill.macwire.wire

    implicit def hint[A]: ProductHint[A] = ProductHint[A](ConfigFieldMapping(CamelCase, CamelCase))

    implicit lazy val investApiModeConvert: ConfigReader[InvestApiMode] = deriveEnumerationReader[InvestApiMode]
    implicit lazy val appConfig: AppConfig = ConfigSource.default.loadOrThrow[AppConfig]

    lazy val investApiClient: InvestApiClient = mock[InvestApiClient]
    lazy val connection: Connection = mock[Connection]
    lazy val shareDAO: ShareDAO = mock[ShareDAO]
    lazy val historicCandleService: CandleService = wire[CandleServiceImpl]
    lazy val priceService: PriceService = wire[PriceServiceImpl]
    lazy val accountService: AccountService = wire[AccountServiceImpl]
    lazy val shareService: ShareService = wire[ShareServiceImpl]
    lazy val fixtureParam: FixtureParam = wire[FixtureParam]

    withFixture(test.toNoArgTest(fixtureParam))
  }

  def generateFakeShareModel(figi: String,
                             purchasePrice: Int,
                             currentPrice: Int)
                            (implicit appConfig: AppConfig): ShareModel = ShareModel(

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

      val shareService = new ShareServiceImpl(
        f.investApiClient,
        f.connection,
        f.shareDAO,
        f.historicCandleService,
        f.priceService,
        f.accountService
      )

      val shares: Seq[ShareModel] = Seq(
        ("keep1", 18, 20),
        ("keep2", 20, 18),
        ("sell1", 20, 18),
        ("sell2", 10, 20)
      ).map(s => generateFakeShareModel(s._1, s._2, s._3))

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
