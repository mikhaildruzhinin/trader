package ru.mikhaildruzhinin.trader.client

import com.typesafe.scalalogging.Logger
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.tinkoff.piapi.contract.v1._
import ru.tinkoff.piapi.core.InvestApi

import java.time.Instant
import scala.jdk.CollectionConverters._

class InvestApiClient(investApi: InvestApi)
                     (implicit appConfig: AppConfig) extends BaseInvestApiClient {

  val log: Logger = Logger(getClass.getName)

  override def getCandles(figi: String,
                          from: Instant,
                          to: Instant,
                          interval: CandleInterval): List[HistoricCandle] = investApi
    .getMarketDataService
    .getCandlesSync(figi, from, to, interval)
    .asScala
    .toList

  override def getShares: List[Share] = investApi
    .getInstrumentsService
    .getSharesSync(InstrumentStatus.INSTRUMENT_STATUS_BASE)
    .asScala
    .toList

  override def getLastPrices(figi: Seq[String]): Seq[LastPrice] = investApi
    .getMarketDataService
    .getLastPricesSync(figi.asJava)
    .asScala
    .toList
}
