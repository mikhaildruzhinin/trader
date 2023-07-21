package ru.mikhaildruzhinin.trader.core.handlers
import ru.mikhaildruzhinin.trader.client.BaseInvestApiClient
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.mikhaildruzhinin.trader.core.TypeCode._
import ru.mikhaildruzhinin.trader.core.wrappers.{HistoricCandleWrapper, ShareWrapper}
import ru.mikhaildruzhinin.trader.database.connection.Connection
import ru.mikhaildruzhinin.trader.database.tables.SharesTable
import ru.tinkoff.piapi.contract.v1.{CandleInterval, Share}

import java.time.{DayOfWeek, LocalDate}

object AvailabilityHandler extends Handler {
  /**
   * Filters shares based on availability filters.
   *
   * @param shares          sequence of shares
   * @param appConfig       application configuration
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
   * @param shares          sequence of shares
   * @param appConfig       application configuration
   * @param investApiClient Tinkoff invest API client
   * @return sequence of wrapped shares
   */
  private def wrapShares(shares: Seq[Share])
                        (implicit appConfig: AppConfig,
                         investApiClient: BaseInvestApiClient): Seq[ShareWrapper] = {

    shares.map(s => {
      val candle = HistoricCandleWrapper(
        investApiClient.getCandles(
          figi = s.getFigi,
          from = appConfig.exchange.startInstantFrom,
          to = appConfig.exchange.startInstantTo,
          interval = CandleInterval.CANDLE_INTERVAL_5_MIN
        ).headOption
      )

      ShareWrapper
        .builder()
        .fromShare(s)
        .withStartingPrice(candle.open)
        .withUpdateTime(candle.time)
        .build()
    })
  }

  /**
   * Writes available shares data to the database.
   *
   * @param appConfig       application configuration
   * @param investApiClient Tinkoff invest API client
   * @param connection      database connection
   * @return number of inserted shares
   */
  override def apply()(implicit appConfig: AppConfig,
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
}
