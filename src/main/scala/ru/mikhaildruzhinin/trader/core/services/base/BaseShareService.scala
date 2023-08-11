package ru.mikhaildruzhinin.trader.core.services.base

import ru.mikhaildruzhinin.trader.core.wrappers.{HistoricCandleWrapper, ShareWrapper}

import scala.concurrent.Future

trait BaseShareService {
  def getAvailableShares: Future[Seq[ShareWrapper]]

  def getUpdatedShares(shares: Seq[ShareWrapper],
                       candles: Seq[HistoricCandleWrapper]): Future[Seq[ShareWrapper]]

  def persistShares(shares: Seq[ShareWrapper]): Future[Option[Int]]
}
