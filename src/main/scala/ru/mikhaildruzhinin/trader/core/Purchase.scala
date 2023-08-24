package ru.mikhaildruzhinin.trader.core

import com.typesafe.scalalogging.Logger
import ru.mikhaildruzhinin.trader.core.services.Services

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Purchase {
  val log: Logger = Logger(getClass.getName)

  def apply(services: Services): Future[Int] = for {

    shares <- services.shareService.getAvailableShares
    candles <- services.historicCandleService.getWrappedCandles(shares)
    availableShares <- services.shareService.getUpdatedShares(shares, candles)
    numAvailableShares <- services.shareService.persistNewShares(availableShares, TypeCode.Available)
    _ <- Future(log.info(s"Available: ${numAvailableShares.getOrElse(0)}"))

    currentPrices <- services.priceService.getCurrentPrices(availableShares)
    updatedShares <- services.shareService.updateCurrentPrices(availableShares, currentPrices)
    numUpdatedShares <- services.shareService.persistUpdatedShares(updatedShares, TypeCode.Available)
    _ <- Future(log.info(s"Updated: ${numUpdatedShares.sum}"))

    uptrendShares <- services.shareService.filterUptrend(updatedShares)
    numUptrendShares <- services.shareService.persistUpdatedShares(uptrendShares, TypeCode.Uptrend)
    _ <- Future(log.info(s"Best uptrend: ${numUptrendShares.sum}"))

    account <- services.accountService.getAccount
    mockPurchasePrices <- Future(uptrendShares.map(_.currentPrice))
    purchasedShares <- services.shareService.updatePurchasePrices(uptrendShares, mockPurchasePrices)
    numPurchasedShares <- services.shareService.persistUpdatedShares(purchasedShares, TypeCode.Purchased)
    _ <- Future(log.info(s"Purchased: ${numPurchasedShares.sum}"))
  } yield numPurchasedShares.sum
}
