package ru.mikhaildruzhinin.trader.core.services.impl

import com.typesafe.scalalogging.Logger
import ru.mikhaildruzhinin.trader.client.base.BaseInvestApiClient
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.mikhaildruzhinin.trader.core.TypeCode
import ru.mikhaildruzhinin.trader.core.services.base.BaseShareService
import ru.mikhaildruzhinin.trader.core.wrappers.{HistoricCandleWrapper, PriceWrapper, ShareWrapper}
import ru.mikhaildruzhinin.trader.database.connection.Connection
import ru.mikhaildruzhinin.trader.database.tables.ShareDAO
import ru.tinkoff.piapi.contract.v1.{Quotation, Share}
import ru.tinkoff.piapi.core.utils.MapperUtils.quotationToBigDecimal

import java.time.{DayOfWeek, LocalDate}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.math.BigDecimal.javaBigDecimal2bigDecimal

class ShareService(investApiClient: BaseInvestApiClient,
                   connection: Connection,
                   shareDAO: ShareDAO)
                  (implicit appConfig: AppConfig) extends BaseShareService {

  protected val log: Logger = Logger(getClass.getName)

  /**
   * Wraps each share in a sequence in an instance of ShareWrapper class.
   *
   * @param shares sequence of shares
   * @return sequence of wrapped shares
   */
  //noinspection ScalaWeakerAccess
  protected def wrapShares(shares: Seq[ShareWrapper],
                           candles: Seq[HistoricCandleWrapper]): Future[Seq[ShareWrapper]] = Future {
    shares
      .zip(candles)
      .map(x => {
        ShareWrapper
          .builder()
          .fromWrapper(x._1)
          .withStartingPrice(x._2.open)
          .withUpdateTime(x._2.time)
          .build()
      })
  }

  //noinspection ScalaWeakerAccess
  protected def filterShares(shares: Seq[Share]): Future[Seq[Share]] = Future {
    shares.filter(s => {
      val isAvailable: Boolean = appConfig
        .exchange
        .names
        .contains(s.getExchange) &&
        s.getApiTradeAvailableFlag &&
        s.getBuyAvailableFlag &&
        s.getSellAvailableFlag /*&&
        !s.getForQualInvestorFlag */

      LocalDate.now.getDayOfWeek match {
        case DayOfWeek.SATURDAY => isAvailable && s.getWeekendFlag
        case DayOfWeek.SUNDAY => isAvailable && s.getWeekendFlag
        case _ => isAvailable
      }
    })
  }

  override def startUp(): Unit = connection.asyncRun(shareDAO.createIfNotExists)

  override def getAvailableShares: Future[Seq[ShareWrapper]] = for {
    shares <- investApiClient.getShares
    filteredShares <- filterShares(shares)
    wrappedShares <- Future { filteredShares.map(s => ShareWrapper.builder().fromShare(s).build()) }
  } yield wrappedShares

  override def getUpdatedShares(shares: Seq[ShareWrapper],
                       candles: Seq[HistoricCandleWrapper]): Future[Seq[ShareWrapper]] = for {
    wrappedShares <- wrapShares(shares, candles)
  } yield wrappedShares

  override def persistNewShares(shares: Seq[ShareWrapper],
                       typeCode: TypeCode): Future[Option[Int]] = for {
    _ <- connection.asyncRun(shareDAO.delete())
    insertedShares <- connection.asyncRun(
      shareDAO.insert(
        shares.map(_.toShareType(typeCode))
      )
    )
  } yield insertedShares

  override def getPersistedShares(typeCode: TypeCode): Future[Seq[ShareWrapper]] = for {
    shareModels <- connection.asyncRun(shareDAO.filterByTypeCode(typeCode.code))
    shares <- Future { shareModels.map( s => ShareWrapper.builder().fromModel(s).build()) }
  } yield shares

  override def updateCurrentPrices(shares: Seq[ShareWrapper],
                                   prices: Seq[PriceWrapper]): Future[Seq[ShareWrapper]] = Future {
    prices.zip(shares)
      .map(x =>
        ShareWrapper
          .builder()
          .fromWrapper(x._2)
          .withCurrentPrice(Some(x._1.price))
          .withUpdateTime(Some(x._1.updateTime))
          .build()
      )
  }

  override def updatePurchasePrices(shares: Seq[ShareWrapper],
                                    prices: Seq[Option[Quotation]]): Future[Seq[ShareWrapper]] = Future {
    shares.zip(prices)
      .map(
      x => ShareWrapper
        .builder()
        .fromWrapper(x._1)
        .withPurchasePrice(x._2)
        .build()
    )
  }

  override def filterUptrend(shares: Seq[ShareWrapper]): Future[Seq[ShareWrapper]] = Future {

    shares
      .filter(_.uptrendPct >= Some(appConfig.shares.uptrendThresholdPct))
      .filter(s => {
        quotationToBigDecimal(
          s.currentPrice.getOrElse(Quotation.newBuilder.build())
        ) * s.lot <= BigDecimal(appConfig.shares.totalPriceLimit)
      })
      .sortBy(_.uptrendAbs)
      .reverse
      .take(appConfig.shares.numUptrendShares)
  }

  override def persistUpdatedShares(shares: Seq[ShareWrapper],
                                    typeCode: TypeCode): Future[Seq[Int]] = connection.asyncRun(
    connection.databaseConfig
      .profile
      .api
      .DBIO
      .sequence(
        shares.map(s => shareDAO.update(s.figi, s.toShareType(typeCode)))
      )
  )
}
