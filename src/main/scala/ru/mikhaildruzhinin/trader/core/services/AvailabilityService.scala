package ru.mikhaildruzhinin.trader.core.services

import com.typesafe.scalalogging.Logger
import ru.mikhaildruzhinin.trader.client.BaseInvestApiClient
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.mikhaildruzhinin.trader.core.TypeCode._
import ru.mikhaildruzhinin.trader.core.wrappers.{HistoricCandleWrapper, ShareWrapper}
import ru.mikhaildruzhinin.trader.database.connection.Connection
import ru.mikhaildruzhinin.trader.database.tables.SharesTable
import ru.tinkoff.piapi.contract.v1.{CandleInterval, HistoricCandle, Share}

import java.time.{DayOfWeek, LocalDate}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AvailabilityService(implicit appConfig: AppConfig,
                          investApiClient: BaseInvestApiClient,
                          connection: Connection) {

  val log: Logger = Logger(getClass.getName)

  /**
   * Filters shares based on availability filters.
   *
   * @param share sequence of shares
   * @return sequence of filtered shares
   */
  private def filterAvailableShares(share: Share): Boolean = {

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

  private def getCandles(shares: Seq[Share]): Future[Seq[Option[HistoricCandle]]] = Future.sequence(
    shares.map(s => investApiClient.getCandles(
      figi = s.getFigi,
      from = appConfig.exchange.startInstantFrom,
      to = appConfig.exchange.startInstantTo,
      interval = CandleInterval.CANDLE_INTERVAL_5_MIN
    ).map(_.headOption))
  )

  /**
   * Wraps each share in a sequence in an instance of ShareWrapper class.
   *
   * @param shares sequence of shares
   * @return sequence of wrapped shares
   */
  private def wrapShares(shares: Seq[Share],
                         candles: Seq[HistoricCandleWrapper]): Future[Seq[ShareWrapper]] = Future {
    shares
      .zip(candles)
      .map(x => {
        ShareWrapper
          .builder()
          .fromShare(x._1)
          .withStartingPrice(x._2.open)
          .withUpdateTime(x._2.time)
          .build()
      })
  }

  private def getFilteredShares(shares: Seq[Share]): Future[Seq[Share]] = Future {
    shares.filter(s => filterAvailableShares(s))
  }

  private def wrapCandles(candles: Seq[Option[HistoricCandle]]): Future[Seq[HistoricCandleWrapper]] = Future {
    candles.map(c => HistoricCandleWrapper(c))
  }

  /**
   * Writes available shares data to the database.
   *
   * @return number of inserted shares
   */
  def getAvailableShares: Future[Option[Int]] = for {
      shares <- investApiClient.getShares
      filteredShares <- getFilteredShares(shares)
      candles <- getCandles(filteredShares)
      wrappedCandles <- wrapCandles(candles)
      wrappedShares <- wrapShares(filteredShares, wrappedCandles)
      _ <- connection.asyncRun(SharesTable.delete())
      insertedShares <- connection.asyncRun(
        SharesTable.insert(
          wrappedShares.map(_.toShareType(Available))
        )
      )
    } yield insertedShares
}
