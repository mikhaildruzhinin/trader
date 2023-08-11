package ru.mikhaildruzhinin.trader.core.services

import com.typesafe.scalalogging.Logger
import ru.mikhaildruzhinin.trader.client.BaseInvestApiClient
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.mikhaildruzhinin.trader.core.TypeCode._
import ru.mikhaildruzhinin.trader.core.services.base.BaseShareService
import ru.mikhaildruzhinin.trader.core.wrappers.{HistoricCandleWrapper, ShareWrapper}
import ru.mikhaildruzhinin.trader.database.connection.Connection
import ru.mikhaildruzhinin.trader.database.tables.SharesTable
import ru.tinkoff.piapi.contract.v1.Share

import java.time.{DayOfWeek, LocalDate}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ShareService(investApiClient: BaseInvestApiClient,
                   connection: Connection)
                  (implicit appConfig: AppConfig) extends BaseShareService {

  protected val log: Logger = Logger(getClass.getName)

  /**
   * Filters shares based on availability filters.
   *
   * @param share sequence of shares
   * @return sequence of filtered shares
   */
  protected def filterAvailableShares(share: Share): Boolean = {

    val isAvailable: Boolean = appConfig
      .exchange
      .names
      .contains(share.getExchange) &&
      share.getApiTradeAvailableFlag &&
      share.getBuyAvailableFlag &&
      share.getSellAvailableFlag

    LocalDate.now.getDayOfWeek match {
      case DayOfWeek.SATURDAY => isAvailable && share.getWeekendFlag
      case DayOfWeek.SUNDAY => isAvailable && share.getWeekendFlag
      case _ => isAvailable
    }
  }

  /**
   * Wraps each share in a sequence in an instance of ShareWrapper class.
   *
   * @param shares sequence of shares
   * @return sequence of wrapped shares
   */
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

  protected def filterShares(shares: Seq[Share]): Future[Seq[Share]] = Future {
    shares.filter(s => {
      val isAvailable: Boolean = appConfig
        .exchange
        .names
        .contains(s.getExchange) &&
        s.getApiTradeAvailableFlag &&
        s.getBuyAvailableFlag &&
        s.getSellAvailableFlag

      LocalDate.now.getDayOfWeek match {
        case DayOfWeek.SATURDAY => isAvailable && s.getWeekendFlag
        case DayOfWeek.SUNDAY => isAvailable && s.getWeekendFlag
        case _ => isAvailable
      }
    })
  }

  def getAvailableShares: Future[Seq[ShareWrapper]] = for {
    shares <- investApiClient.getShares
    filteredShares <- filterShares(shares)
    wrappedShares <- Future { filteredShares.map(s => ShareWrapper.builder().fromShare(s).build()) }
  } yield wrappedShares

  def getUpdatedShares(shares: Seq[ShareWrapper],
                       candles: Seq[HistoricCandleWrapper]): Future[Seq[ShareWrapper]] = for {
    wrappedShares <- wrapShares(shares, candles)
  } yield wrappedShares

  def persistShares(shares: Seq[ShareWrapper]): Future[Option[Int]] = for {
    _ <- connection.asyncRun(SharesTable.delete())
    insertedShares <- connection.asyncRun(
      SharesTable.insert(
        shares.map(_.toShareType(Available))
      )
    )
  } yield insertedShares
}
