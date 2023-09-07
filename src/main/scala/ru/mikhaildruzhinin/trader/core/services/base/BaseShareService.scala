package ru.mikhaildruzhinin.trader.core.services.base

import ru.mikhaildruzhinin.trader.core.TypeCode
import ru.mikhaildruzhinin.trader.core.dto.ShareDTO

import scala.concurrent.Future

trait BaseShareService {
  type EnrichedShareDTO = (ShareDTO, Option[BigDecimal])

  def getFilteredShares: Future[Seq[ShareDTO]]

  def getPersistedShares(typeCode: TypeCode): Future[Seq[ShareDTO]]

  def getAvailableShares: Future[Seq[ShareDTO]]

  def purchaseUptrendShares(): Future[Int]

  def monitorPurchasedShares(): Future[Int]

  def sellShares(): Future[Int]
}
