package ru.mikhaildruzhinin.trader.core.services.base

import ru.mikhaildruzhinin.trader.core.TypeCode
import ru.mikhaildruzhinin.trader.core.wrappers.{HistoricCandleWrapper, PriceWrapper, ShareWrapper}
import ru.tinkoff.piapi.contract.v1.Quotation

import scala.concurrent.Future

trait BaseShareService {
  def startUp(): Unit

  def getAvailableShares: Future[Seq[ShareWrapper]]

  def getUpdatedShares(shares: Seq[ShareWrapper],
                       candles: Seq[HistoricCandleWrapper]): Future[Seq[ShareWrapper]]

  def persistNewShares(shares: Seq[ShareWrapper], typeCode: TypeCode): Future[Option[Int]]

  def getPersistedShares(typeCode: TypeCode): Future[Seq[ShareWrapper]]

  def updateCurrentPrices(shares: Seq[ShareWrapper], prices: Seq[PriceWrapper]): Future[Seq[ShareWrapper]]

  def updatePurchasePrices(shares: Seq[ShareWrapper], prices: Seq[Option[Quotation]]): Future[Seq[ShareWrapper]]

  def filterUptrend(shares: Seq[ShareWrapper]): Future[Seq[ShareWrapper]]

  def persistUpdatedShares(shares: Seq[ShareWrapper], typeCode: TypeCode): Future[Seq[Int]]
}
