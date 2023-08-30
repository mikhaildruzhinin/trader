package ru.mikhaildruzhinin.trader.core

import com.typesafe.scalalogging.Logger
import ru.mikhaildruzhinin.trader.core.services.Services

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Monitor {
  val log: Logger = Logger(getClass.getName)

  def apply(services: Services): Future[Int] = for {
    shares <- services.shareService.getPersistedShares(TypeCode.Purchased)
    prices <- services.priceService.getCurrentPrices(shares)
    updatedShares <- services.shareService.updateCurrentPrices(shares, prices)
    enrichedShares <- services.shareService.enrichShares(updatedShares)
    (sell, keep) <- services.shareService.partitionEnrichedSharesShares(enrichedShares)
    soldSharesNum <- services.shareService.persistUpdatedShares(sell.map(_._1), TypeCode.Sold)
    _ <- Future(log.info(s"Sold: ${soldSharesNum.sum}"))
    _ <- Future(sell.foreach(s => log.info(s.toString)))
    _ <- services.shareService.persistUpdatedShares(keep.map(_._1), TypeCode.Purchased)
  } yield soldSharesNum.sum
}
