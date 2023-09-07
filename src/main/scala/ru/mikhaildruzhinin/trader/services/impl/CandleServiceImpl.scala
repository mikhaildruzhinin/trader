package ru.mikhaildruzhinin.trader.services.impl

import ru.mikhaildruzhinin.trader.client.InvestApiClient
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.mikhaildruzhinin.trader.models.{CandleModel, ShareModel}
import ru.mikhaildruzhinin.trader.services.CandleService
import ru.tinkoff.piapi.contract.v1.{CandleInterval, HistoricCandle}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CandleServiceImpl(investApiClient: InvestApiClient)
                       (implicit appConfig: AppConfig) extends CandleService {

  protected def getCandles(shares: Seq[ShareModel]): Future[Seq[Seq[HistoricCandle]]] = Future
    .sequence(
      shares.map(s => investApiClient.getCandles(
          figi = s.figi,
          from = appConfig.exchange.startInstantFrom,
          to = appConfig.exchange.startInstantTo,
          interval = CandleInterval.CANDLE_INTERVAL_5_MIN
        ))
    )

  override def getWrappedCandles(shares: Seq[ShareModel]): Future[Seq[CandleModel]] = for {
    candles <- getCandles(shares)
//    _ <- Future { shares.zip(candles).foreach(x => println(x._1.name, x._2.length)) }
    firstCandles <- Future { candles.map(_.headOption) }
    wrappedCandles <- Future { firstCandles.map (c => CandleModel(c)) }
  } yield wrappedCandles
}
