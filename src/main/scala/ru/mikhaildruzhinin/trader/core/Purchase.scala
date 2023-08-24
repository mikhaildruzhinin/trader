package ru.mikhaildruzhinin.trader.core

import com.typesafe.scalalogging.Logger
import ru.mikhaildruzhinin.trader.core.services.Services
import ru.mikhaildruzhinin.trader.core.wrappers.ShareWrapper

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Purchase {
  val log: Logger = Logger(getClass.getName)

  private def getAvailableShares(services: Services): Future[Seq[ShareWrapper]] = for {

    shares <- services.shareService.getAvailableShares
    candles <- services.historicCandleService.getWrappedCandles(shares)
    availableShares <- services.shareService.getUpdatedShares(shares, candles)
    numAvailableShares <- services.shareService.persistNewShares(availableShares, TypeCode.Available)
    _ <- Future(log.info(s"Available: ${numAvailableShares.getOrElse(0)}"))
  } yield availableShares

  private def getUpdatedShares(services: Services,
                               availableShares: Seq[ShareWrapper]): Future[Seq[ShareWrapper]] = for {

    currentPrices <- services.priceService.getCurrentPrices(availableShares)
    updatedShares <- services.shareService.updateCurrentPrices(availableShares, currentPrices)
    numUpdatedShares <- services.shareService.persistUpdatedShares(updatedShares, TypeCode.Available)
    _ <- Future(log.info(s"Updated: ${numUpdatedShares.sum}"))
  } yield updatedShares

  private def getUptrendShares(services: Services,
                               updatedShares: Seq[ShareWrapper]): Future[Seq[ShareWrapper]] = for {
    uptrendShares <- services.shareService.filterUptrend(updatedShares)
    numUptrendShares <- services.shareService.persistUpdatedShares(uptrendShares, TypeCode.Uptrend)
    _ <- Future(log.info(s"Best uptrend: ${numUptrendShares.sum}"))
  } yield uptrendShares

  private def getPurchasedShares(services: Services,
                                 uptrendShares: Seq[ShareWrapper]): Future[Seq[ShareWrapper]] = for {
    account <- services.accountService.getAccount
    mockPurchasePrices <- Future(uptrendShares.map(_.currentPrice))
    purchasedShares <- services.shareService.updatePurchasePrices(uptrendShares, mockPurchasePrices)
    numPurchasedShares <- services.shareService.persistUpdatedShares(purchasedShares, TypeCode.Purchased)
    _ <- Future(log.info(s"Purchased: ${numPurchasedShares.sum}"))
  } yield purchasedShares

  def apply(services: Services): Future[Unit] = for {
    availableShares <- getAvailableShares(services)
    updatedShares <- getUpdatedShares(services, availableShares)
    uptrendShares <- getUptrendShares(services, updatedShares)
    purchasedShares <- getPurchasedShares(services, uptrendShares)
  } yield ()
}
