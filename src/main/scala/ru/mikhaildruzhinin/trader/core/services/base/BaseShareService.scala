package ru.mikhaildruzhinin.trader.core.services.base

import ru.mikhaildruzhinin.trader.core.TypeCode
import ru.mikhaildruzhinin.trader.core.dto.{HistoricCandleDTO, PriceDTO, ShareDTO}
import ru.tinkoff.piapi.contract.v1.Quotation

import scala.concurrent.Future

trait BaseShareService {
  type EnrichedShareWrapper = (ShareDTO, Option[BigDecimal])

  def getAvailableShares: Future[Seq[ShareDTO]]

  def getUpdatedShares(shares: Seq[ShareDTO],
                       candles: Seq[HistoricCandleDTO]): Future[Seq[ShareDTO]]

  def persistNewShares(shares: Seq[ShareDTO], typeCode: TypeCode): Future[Option[Int]]

  def getPersistedShares(typeCode: TypeCode): Future[Seq[ShareDTO]]

  def updateCurrentPrices(shares: Seq[ShareDTO], prices: Seq[PriceDTO]): Future[Seq[ShareDTO]]

  def updatePurchasePrices(shares: Seq[ShareDTO], prices: Seq[Option[Quotation]]): Future[Seq[ShareDTO]]

  def filterUptrend(shares: Seq[ShareDTO]): Future[Seq[ShareDTO]]

  def persistUpdatedShares(shares: Seq[ShareDTO], typeCode: TypeCode): Future[Seq[Int]]

  def enrichShares(shares: Seq[ShareDTO]): Future[Seq[EnrichedShareWrapper]]

  def partitionEnrichedSharesShares(enrichedShares: Seq[EnrichedShareWrapper]): Future[(Seq[EnrichedShareWrapper], Seq[EnrichedShareWrapper])]
}
