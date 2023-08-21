package ru.mikhaildruzhinin.trader.core.executables

import com.typesafe.scalalogging.Logger
import ru.mikhaildruzhinin.trader.core.TypeCode
import ru.mikhaildruzhinin.trader.core.services.base._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object PurchaseExecutable {

  val log: Logger = Logger(getClass.getName)

  def apply(shareService: BaseShareService,
            historicCandleService: BaseHistoricCandleService,
            priceService: BasePriceService,
            accountService: BaseAccountService): Future[Int] = for {

    shares <- shareService.getAvailableShares
    candles <- historicCandleService.getWrappedCandles(shares)
    availableShares <- shareService.getUpdatedShares(shares, candles)
    numAvailableShares <- shareService.persistNewShares(availableShares, TypeCode.Available)
    _ <- Future(log.info(s"Available: ${numAvailableShares.getOrElse(0)}"))

    currentPrices <- priceService.getCurrentPrices(availableShares)
    updatedShares <- shareService.updateCurrentPrices(availableShares, currentPrices)
    numUpdatedShares <- shareService.persistUpdatedShares(updatedShares, TypeCode.Available)
    _ <- Future(log.info(s"Updated: ${numUpdatedShares.sum}"))

    uptrendShares <- shareService.filterUptrend(updatedShares)
    numUptrendShares <- shareService.persistUpdatedShares(uptrendShares, TypeCode.Uptrend)
    _ <- Future(log.info(s"Best uptrend: ${numUptrendShares.sum}"))

    account <- accountService.getAccount
    mockPurchasePrices <- Future(uptrendShares.map(_.currentPrice))
    purchasedShares <- shareService.updatePurchasePrices(uptrendShares, mockPurchasePrices)
    numPurchasedShares <- shareService.persistUpdatedShares(purchasedShares, TypeCode.Purchased)
    _ <- Future(log.info(s"Purchased: ${numPurchasedShares.sum}"))
  } yield numPurchasedShares.sum
}
