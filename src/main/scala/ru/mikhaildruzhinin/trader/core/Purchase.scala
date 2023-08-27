package ru.mikhaildruzhinin.trader.core

import com.typesafe.scalalogging.Logger
import ru.mikhaildruzhinin.trader.core.services.Services
import ru.mikhaildruzhinin.trader.core.wrappers.ShareWrapper

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Purchase {
  val log: Logger = Logger(getClass.getName)

  private def persistAvailableShares(services: Services): Future[(Seq[ShareWrapper], Int)] = for {

    shares <- services.shareService.getAvailableShares
    candles <- services.historicCandleService.getWrappedCandles(shares)
    availableShares <- services.shareService.getUpdatedShares(shares, candles)
    numAvailableShares <- services.shareService.persistNewShares(availableShares, TypeCode.Available)
    _ <- Future(log.info(s"Available: ${numAvailableShares.getOrElse(0)}"))
  } yield (availableShares, numAvailableShares.getOrElse(0))

  private def persistUpdatedShares(services: Services,
                                   availableShares: Seq[ShareWrapper]): Future[(Seq[ShareWrapper], Int)] = for {

    currentPrices <- services.priceService.getCurrentPrices(availableShares)
    updatedShares <- services.shareService.updateCurrentPrices(availableShares, currentPrices)
    numUpdatedShares <- services.shareService.persistUpdatedShares(updatedShares, TypeCode.Available)
    _ <- Future(log.info(s"Updated: ${numUpdatedShares.sum}"))
  } yield (updatedShares, numUpdatedShares.sum)

  private def persistUptrendShares(services: Services,
                                   updatedShares: Seq[ShareWrapper]): Future[(Seq[ShareWrapper], Int)] = for {
    uptrendShares <- services.shareService.filterUptrend(updatedShares)
    numUptrendShares <- services.shareService.persistUpdatedShares(uptrendShares, TypeCode.Uptrend)
    _ <- Future(log.info(s"Best uptrend: ${numUptrendShares.sum}"))
  } yield (uptrendShares, numUptrendShares.sum)

  private def persistPurchasedShares(services: Services,
                                     uptrendShares: Seq[ShareWrapper]): Future[(Seq[ShareWrapper], Int)] = for {
    account <- services.accountService.getAccount
    mockPurchasePrices <- Future(uptrendShares.map(_.currentPrice))
    purchasedShares <- services.shareService.updatePurchasePrices(uptrendShares, mockPurchasePrices)
    numPurchasedShares <- services.shareService.persistUpdatedShares(purchasedShares, TypeCode.Purchased)
    _ <- Future(log.info(s"Purchased: ${numPurchasedShares.sum}"))
  } yield (purchasedShares, numPurchasedShares.sum)

  def apply(services: Services): Future[Unit] = for {
    (availableShares, _) <- persistAvailableShares(services)
    (updatedShares, _) <- persistUpdatedShares(services, availableShares)
    (uptrendShares, _) <- persistUptrendShares(services, updatedShares)
    (purchasedShares, numPurchasedShares) <- persistPurchasedShares(services, uptrendShares)
  } yield numPurchasedShares
}
