package ru.mikhaildruzhinin.trader.core.handlers
import ru.mikhaildruzhinin.trader.client.BaseInvestApiClient
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.mikhaildruzhinin.trader.core.TypeCode._
import ru.mikhaildruzhinin.trader.core.wrappers.ShareWrapper
import ru.mikhaildruzhinin.trader.database.connection.Connection
import ru.mikhaildruzhinin.trader.database.tables.SharesTable
import ru.tinkoff.piapi.contract.v1.Quotation
import ru.tinkoff.piapi.core.InvestApi
import ru.tinkoff.piapi.core.utils.MapperUtils.quotationToBigDecimal
import slick.dbio.DBIO

import scala.annotation.tailrec
import scala.math.BigDecimal.javaBigDecimal2bigDecimal

object UptrendHandler extends Handler {
  private def filterUptrendShares(shares: Seq[ShareWrapper])
                                 (implicit appConfig: AppConfig,
                                  investApiClient: BaseInvestApiClient,
                                  connection: Connection): Seq[ShareWrapper] = {

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

  @tailrec
  private def getUptrendShares(numAttempt: Int = 1)
                              (implicit appConfig: AppConfig,
                           investApi: InvestApi,
                           investApiClient: BaseInvestApiClient,
                           connection: Connection): Seq[ShareWrapper] = {

    val maxNumAttempts: Int = 3
    log.info(s"Attempt $numAttempt of $maxNumAttempts")

    val shares: Seq[ShareWrapper] = wrapPersistedShares(Available)
    val updatedShares: Seq[ShareWrapper] = updateCurrentPrices(shares)
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
                       investApi: InvestApi,
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
