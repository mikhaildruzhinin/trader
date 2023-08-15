package ru.mikhaildruzhinin.trader.client.impl

import com.typesafe.scalalogging.Logger
import ru.mikhaildruzhinin.trader.client.base.BaseInvestApiClient
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.tinkoff.piapi.contract.v1._
import ru.tinkoff.piapi.core.InvestApi

import java.time.Instant
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.jdk.CollectionConverters._
import scala.jdk.FutureConverters._

class InvestApiClient(investApi: InvestApi)
                     (implicit appConfig: AppConfig) extends BaseInvestApiClient {

  val log: Logger = Logger(getClass.getName)

  override def getCandles(figi: String,
                          from: Instant,
                          to: Instant,
                          interval: CandleInterval): Future[Seq[HistoricCandle]] = investApi
    .getMarketDataService
    .getCandles(figi, from, to, interval)
    .asScala
    .map(_.asScala.toSeq)

  override def getShares: Future[Seq[Share]] = investApi
    .getInstrumentsService
    .getShares(InstrumentStatus.INSTRUMENT_STATUS_BASE)
    .asScala
    .map(_.asScala.toSeq)

  override def getLastPrices(figi: Seq[String]): Future[Seq[LastPrice]] = investApi
    .getMarketDataService
    .getLastPrices(figi.asJava)
    .asScala
    .map(_.asScala.toSeq)

  override def getAccount: Future[Account] = investApi
    .getUserService
    .getAccounts
    .asScala
    .map(_.asScala.head)

  override def postOrder(figi: String,
                         quantity: Long,
                         price: Quotation,
                         direction: OrderDirection,
                         accountId: String,
                         orderType: OrderType,
                         orderId: UUID): Future[PostOrderResponse] = investApi
    .getOrdersService
    .postOrder(
      figi,
      quantity,
      price,
      direction,
      accountId,
      orderType,
      orderId.toString
    )
    .asScala
}
