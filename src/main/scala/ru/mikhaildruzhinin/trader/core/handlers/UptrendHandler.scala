package ru.mikhaildruzhinin.trader.core.handlers
import ru.mikhaildruzhinin.trader.client.BaseInvestApiClient
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.mikhaildruzhinin.trader.core.{HistoricCandleWrapper, ShareWrapper}
import ru.mikhaildruzhinin.trader.core.TypeCode._
import ru.mikhaildruzhinin.trader.database.connection.Connection
import ru.mikhaildruzhinin.trader.database.tables.SharesTable
import ru.tinkoff.piapi.contract.v1.CandleInterval
import slick.dbio.DBIO

import scala.annotation.tailrec

object UptrendHandler extends Handler {
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

    shares
      .filter(_.uptrendPct >= Some(appConfig.shares.uptrendThresholdPct))
      .sortBy(_.uptrendAbs)
      .reverse
      .take(appConfig.shares.numUptrendShares)
  }

  @tailrec
  def getUptrendShares(numAttempt: Int = 1)
                          (implicit appConfig: AppConfig,
                           investApiClient: BaseInvestApiClient,
                           connection: Connection): Seq[ShareWrapper] = {

    val maxNumAttempts: Int = 3
    log.info(s"Attempt $numAttempt of $maxNumAttempts")

    val shares: Seq[ShareWrapper] = wrapPersistedShares(Available)
    val updatedShares: Seq[ShareWrapper] = updateCurrentPrice(shares)
    val uptrendShares: Seq[ShareWrapper] = filterUptrendShares(updatedShares)

    log.info(s"Best uptrend: ${uptrendShares.length.toString}")

    uptrendShares.length match {
      case l if l > 0 => uptrendShares
      case l => if (numAttempt < maxNumAttempts) {
        Thread.sleep(5 * 60 * 1000)
        getUptrendShares(numAttempt + 1)
      } else uptrendShares
    }
  }

  override def apply()(implicit appConfig: AppConfig,
                       investApiClient: BaseInvestApiClient,
                       connection: Connection): Int = {

    val uptrendShares: Seq[ShareWrapper] = getUptrendShares()

    connection.run(
      DBIO.sequence(uptrendShares.map(s => {
        SharesTable.update(s.figi, s.toShareType(Uptrend))
      }))
    )

    uptrendShares.length
  }
}
