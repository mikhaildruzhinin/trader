package ru.mikhaildruzhinin.trader.core.handlers

import ru.mikhaildruzhinin.trader.client.BaseInvestApiClient
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.mikhaildruzhinin.trader.core.TypeCode._
import ru.mikhaildruzhinin.trader.core.{HistoricCandleWrapper, ShareWrapper}
import ru.mikhaildruzhinin.trader.database.connection.Connection
import ru.mikhaildruzhinin.trader.database.tables.SharesTable
import ru.tinkoff.piapi.contract.v1.{CandleInterval, Share}
import slick.dbio.DBIO

import java.time.{DayOfWeek, LocalDate}
import scala.annotation.tailrec

object PurchaseHandler extends Handler {
  /**
   * Filters shares based on availability filters.
   *
   * @param shares sequence of shares
   * @param appConfig application configuration
   * @param investApiClient Tinkoff invest API client
   * @return sequence of filtered shares
   */
  private def filterAvailableShares(shares: Seq[Share])
                                   (implicit appConfig: AppConfig,
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

      val candle = HistoricCandleWrapper(
        investApiClient.getCandles(
          figi = shareWrapper.figi,
          from = appConfig.exchange.startInstantFrom,
          to = appConfig.exchange.startInstantTo,
          interval = CandleInterval.CANDLE_INTERVAL_5_MIN
        ).headOption
      )

      ShareWrapper(
        shareWrapper = shareWrapper,
        startingPrice = candle.open,
        purchasePrice = None,
        currentPrice = None,
        updateTime = candle.time
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

    val filteredShares = filterAvailableShares(investApiClient.getShares)
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

  private def updateCurrentPrice(shares: Seq[ShareWrapper])
                                (implicit appConfig: AppConfig,
                                 investApiClient: BaseInvestApiClient): Seq[ShareWrapper] = {

    shares.map(s => {
      val candle = HistoricCandleWrapper(
        investApiClient.getCandles(
          figi = s.figi,
          from = appConfig.exchange.updateInstantFrom,
          to = appConfig.exchange.updateInstantTo,
          interval = CandleInterval.CANDLE_INTERVAL_5_MIN
        ).headOption
      )

      ShareWrapper(
        shareWrapper = s,
        startingPrice = s.startingPrice,
        purchasePrice = None,
        currentPrice = candle.close,
        updateTime = candle.time
      )
    })
  }

  private def filterUptrendShares(shares: Seq[ShareWrapper])
                                 (implicit appConfig: AppConfig,
                                  investApiClient: BaseInvestApiClient,
                                  connection: Connection): Seq[ShareWrapper] = {

    shares.filter(_.uptrendPct >= Some(appConfig.shares.uptrendThresholdPct))
      .sortBy(_.uptrendAbs)
      .reverse
      .take(appConfig.shares.numUptrendShares)
  }

  @tailrec
  def persistUptrendShares(numAttempt: Int = 1)
                          (implicit appConfig: AppConfig,
                           investApiClient: BaseInvestApiClient,
                           connection: Connection): Int = {

    val maxNumAttempts: Int = 3
    log.info(s"Attempt $numAttempt of $maxNumAttempts")

    val shares: Seq[ShareWrapper] = wrapPersistedShares(Available)
    val updatedShares: Seq[ShareWrapper] = updateCurrentPrice(shares)
    val uptrendShares: Seq[ShareWrapper] = filterUptrendShares(updatedShares)

    connection.run(
      DBIO.sequence(uptrendShares.map(s => {
        SharesTable.update(s.figi, s.toShareType(Uptrend))
      }))
    )

    log.info(s"Best uptrend: ${uptrendShares.length.toString}")

    uptrendShares.length match {
      case l if l > 0 => l
      case l => if (numAttempt < maxNumAttempts) {
        Thread.sleep(5 * 60 * 1000)
        persistUptrendShares(numAttempt + 1)
      } else l
    }
  }

  def persistPurchasedShares()(implicit appConfig: AppConfig,
                               connection: Connection): Int = {

    val purchasedShares: Seq[ShareWrapper] = wrapPersistedShares(Uptrend)
      .map(
        s => ShareWrapper(
          shareWrapper = s,
          startingPrice = s.startingPrice,
          purchasePrice = s.currentPrice,
          currentPrice = s.currentPrice,
          updateTime = s.updateTime
        )
      )

    connection.run(
      DBIO.sequence(purchasedShares.map(s => {
        SharesTable.update(s.figi, s.toShareType(Purchased))
      }))
    )

    log.info(s"Purchased: ${purchasedShares.length.toString}")
    purchasedShares.length
  }

  override def apply()(implicit appConfig: AppConfig,
                       investApiClient: BaseInvestApiClient,
                       connection: Connection): Int = {

    persistAvailableShares()
    persistUptrendShares()
    persistPurchasedShares()
  }
}
