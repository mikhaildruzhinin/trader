package ru.mikhaildruzhinin.trader.services.impl

import com.typesafe.scalalogging.Logger
import ru.mikhaildruzhinin.trader.TypeCode
import ru.mikhaildruzhinin.trader.client.InvestApiClient
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.mikhaildruzhinin.trader.database.Connection
import ru.mikhaildruzhinin.trader.database.tables.ShareDAO
import ru.mikhaildruzhinin.trader.models.ShareModel.EnrichedShareModel
import ru.mikhaildruzhinin.trader.models.{CandleModel, PriceModel, ShareModel}
import ru.mikhaildruzhinin.trader.services._
import ru.tinkoff.piapi.contract.v1.{Quotation, Share}
import ru.tinkoff.piapi.core.utils.MapperUtils.quotationToBigDecimal

import java.time.{DayOfWeek, LocalDate}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.math.BigDecimal.javaBigDecimal2bigDecimal

class ShareServiceImpl(investApiClient: InvestApiClient,
                       connection: Connection,
                       shareDAO: ShareDAO,
                       candleService: CandleService,
                       priceService: PriceService,
                       accountService: AccountService)
                      (implicit appConfig: AppConfig) extends ShareService {

  protected val log: Logger = Logger(getClass.getName)

  protected def updateStartingPrices(shares: Seq[ShareModel],
                                     candles: Seq[CandleModel]): Future[Seq[ShareModel]] = Future {
    shares
      .zip(candles)
      .map(x => {
        ShareModel
          .builder()
          .fromModel(x._1)
          .withStartingPrice(x._2.open)
          .withUpdateTime(x._2.time)
          .build()
      })
  }

  protected def filterShares(shares: Seq[Share]): Future[Seq[Share]] = Future {
    shares.filter(s => {
      val isAvailable: Boolean = appConfig
        .exchange
        .names
        .contains(s.getExchange) &&
        s.getApiTradeAvailableFlag &&
        s.getBuyAvailableFlag &&
        s.getSellAvailableFlag /*&&
        !s.getForQualInvestorFlag */

      LocalDate.now.getDayOfWeek match {
        case DayOfWeek.SATURDAY => isAvailable && s.getWeekendFlag
        case DayOfWeek.SUNDAY => isAvailable && s.getWeekendFlag
        case _ => isAvailable
      }
    })
  }

  override def getFilteredShares: Future[Seq[ShareModel]] = for {
    shares <- investApiClient.getShares
    filteredShares <- filterShares(shares)
    wrappedShares <- Future { filteredShares.map(s => ShareModel.builder().fromShare(s).build()) }
  } yield wrappedShares

  protected def persistNewShares(shares: Seq[ShareModel],
                                 typeCode: TypeCode): Future[Option[Int]] = for {
    _ <- shareDAO.delete()
    insertedShares <- shareDAO.insert(shares, typeCode)
  } yield insertedShares

  override def getPersistedShares(typeCode: TypeCode): Future[Seq[ShareModel]] = for {
    shares <- shareDAO.filterByTypeCode(typeCode.code, appConfig.testFlg)
  } yield shares

  protected def updateCurrentPrices(shares: Seq[ShareModel],
                                    prices: Seq[PriceModel]): Future[Seq[ShareModel]] = Future {
    prices.zip(shares)
      .map(x =>
        ShareModel
          .builder()
          .fromModel(x._2)
          .withCurrentPrice(Some(x._1.price))
          .withUpdateTime(Some(x._1.updateTime))
          .build()
      )
  }

  protected def calculateQuantities(shares: Seq[ShareModel]): Future[Seq[Option[Int]]] = Future {
    shares.map(s =>
      Some((
        BigDecimal(10000 / shares.length)
          / quotationToBigDecimal(s.currentPrice.getOrElse(Quotation.newBuilder.build()))
          / s.lot
        ).toInt)
    )
  }

  protected def updatePurchasePrices(shares: Seq[ShareModel],
                                     prices: Seq[Option[Quotation]],
                                     quantities: Seq[Option[Int]]): Future[Seq[ShareModel]] = Future {
    shares.zip(prices)
      .zip(quantities)
      .map { case ((s, p), q) => (s, p, q) }
      .map(
        x => ShareModel
          .builder()
          .fromModel(x._1)
          .withPurchasePrice(x._2)
          .withQuantity(x._3)
          .build()
      )
  }

  protected def filterUptrend(shares: Seq[ShareModel]): Future[Seq[ShareModel]] = Future {

    shares
      .filter(_.uptrendPct >= Some(appConfig.shares.uptrendThresholdPct))
      .filter(s => {
        quotationToBigDecimal(
          s.currentPrice.getOrElse(Quotation.newBuilder.build())
        ) * s.lot <= BigDecimal(appConfig.shares.totalPriceLimit)
      })
      .sortBy(_.uptrendAbs)
      .reverse
      .take(appConfig.shares.numUptrendShares)
  }

  protected def persistUpdatedShares(shares: Seq[ShareModel],
                                     typeCode: TypeCode): Future[Seq[Int]] = shareDAO.update(shares, typeCode)

  /*protected*/ def partitionEnrichedSharesShares(enrichedShares: Seq[EnrichedShareModel]): Future[(Seq[EnrichedShareModel], Seq[EnrichedShareModel])] = Future {
    enrichedShares.partition(s =>
      (s._1.roi <= Some(BigDecimal(appConfig.shares.stopLossPct))
        && s._1.roi < s._2)
        || s._1.roi >= Some(BigDecimal(appConfig.shares.takeProfitPct))
    )
  }

  // public or protected?
  override def getAvailableShares: Future[Seq[ShareModel]] = for {
    shares <- getFilteredShares
    candles <- candleService.getWrappedCandles(shares)
    availableShares <- updateStartingPrices(shares, candles)
  } yield availableShares

  protected def purchaseShares(shares: Seq[ShareModel]): Future[Seq[ShareModel]] = for {
    account <- accountService.getAccount
    mockPurchasePrices <- Future(shares.map(_.currentPrice))
    quantities <- calculateQuantities(shares)
    purchasedShares <- updatePurchasePrices(shares, mockPurchasePrices, quantities)
  } yield purchasedShares

  override def purchaseUptrendShares(): Future[Int] = for {
    availableShares <- getAvailableShares
    numAvailableShares <- persistNewShares(availableShares, TypeCode.Available)
    _ <- Future(log.info(s"Available: ${numAvailableShares.getOrElse(0)}"))

    currentPrices <- priceService.getCurrentPrices(availableShares)
    updatedShares <- updateCurrentPrices(availableShares, currentPrices)
    numUpdatedShares <- shareDAO.update(updatedShares, TypeCode.Available)
    _ <- Future(log.info(s"Updated: ${numUpdatedShares.sum}"))

    uptrendShares <- filterUptrend(updatedShares)
    numUptrendShares <- shareDAO.update(uptrendShares, TypeCode.Uptrend)
    _ <- Future(log.info(s"Best uptrend: ${numUptrendShares.sum}"))

    purchasedShares <- purchaseShares(uptrendShares)
    numPurchasedShares <- shareDAO.update(purchasedShares, TypeCode.Purchased)
    _ <- Future(log.info(s"Purchased: ${numPurchasedShares.sum}"))
  } yield purchasedShares.length

  override def monitorPurchasedShares(): Future[Int] = for {
    shares <- getPersistedShares(TypeCode.Purchased)
    prices <- priceService.getCurrentPrices(shares)
    updatedShares <- updateCurrentPrices(shares, prices)
    enrichedShares: Seq[EnrichedShareModel] <- Future(shares.zip(shares.map(_.roi)))
    (sell, keep) <- partitionEnrichedSharesShares(enrichedShares)
    soldSharesNum <- shareDAO.update(sell.map(_._1), TypeCode.Sold)
    _ <- Future(log.info(s"Sold: ${soldSharesNum.sum}"))
    _ <- Future(sell.foreach(s => log.info(s.toString)))
    _ <- shareDAO.update(keep.map(_._1), TypeCode.Purchased)
  } yield soldSharesNum.sum

  override def sellShares(): Future[Int] = for {
    shares <- getPersistedShares(TypeCode.Purchased)
    soldSharesNum <- shareDAO.update(shares, TypeCode.Sold)
    _ <- Future(log.info(s"Sold: ${soldSharesNum.sum}"))
    _ <- Future(shares.foreach(s => log.info(s.toString)))
  } yield soldSharesNum.sum
}
