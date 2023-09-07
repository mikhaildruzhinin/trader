package ru.mikhaildruzhinin.trader.client

import ru.tinkoff.piapi.contract.v1._

import java.time.Instant
import java.util.UUID
import scala.concurrent.Future

abstract class InvestApiClient {

  def getCandles(figi: String,
                 from: Instant,
                 to: Instant,
                 interval: CandleInterval): Future[Seq[HistoricCandle]]

  def getShares: Future[Seq[Share]]

  def getLastPrices(figi: Seq[String]): Future[Seq[LastPrice]]

  def getAccount: Future[Account]

  def postOrder(figi: String,
                quantity: Long,
                price: Quotation = Quotation.newBuilder.build(),
                direction: OrderDirection,
                accountId: String,
                orderType: OrderType,
                orderId: UUID): Future[PostOrderResponse]
}
