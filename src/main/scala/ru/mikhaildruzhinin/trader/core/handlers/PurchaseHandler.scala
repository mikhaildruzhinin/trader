package ru.mikhaildruzhinin.trader.core.handlers

import com.google.protobuf.Timestamp
import ru.mikhaildruzhinin.trader.client.BaseInvestApiClient
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.mikhaildruzhinin.trader.core.ShareWrapper
import ru.mikhaildruzhinin.trader.core.TypeCode._
import ru.mikhaildruzhinin.trader.database.connection.Connection
import ru.mikhaildruzhinin.trader.database.tables.SharesTable
import ru.tinkoff.piapi.contract.v1.{CandleInterval, Quotation, Share}

import java.time.{DayOfWeek, LocalDate}
import scala.annotation.tailrec
import scala.concurrent.Await

object PurchaseHandler extends Handler {
  /**
   * Filters shares based on preset filters.
   *
   * @param shares sequence of shares
   * @param appConfig application configuration
   * @param investApiClient Tinkoff invest API client
   * @return sequence of filtered shares
   */
  private def filterShares(shares: Seq[Share])(implicit appConfig: AppConfig,
                                               investApiClient: BaseInvestApiClient): Seq[Share] = {

    val filteredShares: Seq[Share] = shares
      .filter(
        s =>
          appConfig.exchange.names.contains(s.getExchange) &&
            s.getApiTradeAvailableFlag &&
            s.getBuyAvailableFlag &&
            s.getSellAvailableFlag
      )

    LocalDate.now.getDayOfWeek match {
      case DayOfWeek.SATURDAY => filteredShares.filter(_.getWeekendFlag)
      case DayOfWeek.SUNDAY => filteredShares.filter(_.getWeekendFlag)
      case _ => filteredShares
    }
  }

  /**
   * Wraps each share in a sequence in an instance of ShareWrapper class.
   *
   * @param shares sequence of shares
   * @param appConfig application configuration
   * @param investApiClient Tinkoff invest API client
   * @return sequence of wrapped shares
   */
  private def wrapShares(shares: Seq[Share])
                                (implicit appConfig: AppConfig,
                                 investApiClient: BaseInvestApiClient): Seq[ShareWrapper] = {

    shares.map(s => {
      val shareWrapper = ShareWrapper(s)

      val (
        startingPrice: Option[Quotation],
        _: Option[Quotation],
        updateTime: Option[Timestamp]
      ) = ShareWrapper.getUpdatedCandlePrices(
        shareWrapper = shareWrapper,
        from = appConfig.exchange.startInstantFrom,
        to = appConfig.exchange.startInstantTo,
        interval = CandleInterval.CANDLE_INTERVAL_5_MIN
      )

      ShareWrapper(
        shareWrapper = shareWrapper,
        startingPrice = startingPrice,
        purchasePrice = None,
        currentPrice = None,
        updateTime = updateTime
      )
    })
  }

  /**
   * Writes available shares data to the database.
   *
   * @param appConfig application configuration
   * @param investApiClient Tinkoff invest API client
   * @param connection database connection
   * @return number of inserted shares
   */
  def persistAvailableShares()(implicit appConfig: AppConfig,
                               investApiClient: BaseInvestApiClient,
                               connection: Connection): Int = {

    val filteredShares = filterShares(investApiClient.getShares)
    val wrappedShares: Seq[ShareWrapper] = wrapShares(filteredShares)

    connection.runMultiple(
      Vector(
        SharesTable.delete(),
        SharesTable.insert(wrappedShares.map(_.toShareType(Available))),
      )
    )

    log.info(s"Total: ${wrappedShares.length.toString}")
    wrappedShares.length
  }

  @tailrec
  private def loadUptrendShares(numAttempt: Int = 1)
                               (implicit appConfig: AppConfig,
                                investApiClient: BaseInvestApiClient,
                                connection: Connection): Int = {

    val maxNumAttempts: Int = 3
    log.info(s"Attempt $numAttempt of $maxNumAttempts")
    val uptrendShares: Seq[ShareWrapper] = ShareWrapper
      .getPersistedShares(Available)
      .map(_.updateShare)
//      .filter(_.uptrendPct >= Some(appConfig.shares.uptrendThresholdPct))
      .sortBy(_.uptrendAbs)
      .reverse
      .take(appConfig.shares.numUptrendShares)

    Await.result(
      connection.asyncRun(
        Vector(
          SharesTable.update(figis = uptrendShares.map(s => s.figi), Uptrend.code),
        )
      ),
      appConfig.slick.await.duration
    )

    log.info(s"Best uptrend: ${uptrendShares.length.toString}")

    uptrendShares.length match {
      case x if x > 0 => x
      case x => if (numAttempt < maxNumAttempts) {
        Thread.sleep(5 * 60 * 1000)
        loadUptrendShares(numAttempt + 1)
      } else x
    }
  }

  def purchaseShares()(implicit appConfig: AppConfig,
                       connection: Connection): Int = {

    val purchasedShares: Seq[ShareWrapper] = ShareWrapper
      .getPersistedShares(Uptrend)
      .map(
        s => ShareWrapper(
          shareWrapper = s,
          startingPrice = s.startingPrice,
          purchasePrice = s.currentPrice,
          currentPrice = s.currentPrice,
          updateTime = s.updateTime
        )
      )

    Await.result(
      connection.asyncRun(
        Vector(
          SharesTable.update(figis = purchasedShares.map(s => s.figi), Purchased.code),
        )
      ),
      appConfig.slick.await.duration
    )

    log.info(s"Purchased: ${purchasedShares.length.toString}")
    purchasedShares.length
  }

  override def apply()(implicit appConfig: AppConfig,
                       investApiClient: BaseInvestApiClient,
                       connection: Connection): Int = {

    persistAvailableShares()
    loadUptrendShares()
    purchaseShares()
  }
}
