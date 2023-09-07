package ru.mikhaildruzhinin.trader

import ru.mikhaildruzhinin.trader.config.TypeCode
import ru.mikhaildruzhinin.trader.models._

import scala.concurrent.Future

package object services {
  trait AccountService {
    def getAccount: Future[AccountModel]
  }

  trait CandleService {
    def getWrappedCandles(shares: Seq[ShareModel]): Future[Seq[CandleModel]]
  }

  trait PriceService {
    def getCurrentPrices(shares: Seq[ShareModel]): Future[Seq[PriceModel]]
  }

  trait ShareService {
    type EnrichedShareModel = (ShareModel, Option[BigDecimal])

    def getFilteredShares: Future[Seq[ShareModel]]

    def getPersistedShares(typeCode: TypeCode): Future[Seq[ShareModel]]

    def getAvailableShares: Future[Seq[ShareModel]]

    def purchaseUptrendShares(): Future[Int]

    def monitorPurchasedShares(): Future[Int]

    def sellShares(): Future[Int]
  }
}
