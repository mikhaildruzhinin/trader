package ru.mikhaildruzhinin.trader.core.services.impl

import ru.mikhaildruzhinin.trader.client.base.BaseInvestApiClient
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.mikhaildruzhinin.trader.core.services.base.BaseHistoricCandleService
import ru.mikhaildruzhinin.trader.core.dto.{HistoricCandleDTO, ShareDTO}
import ru.tinkoff.piapi.contract.v1.{CandleInterval, HistoricCandle}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class HistoricCandleService(investApiClient: BaseInvestApiClient)
                           (implicit appConfig: AppConfig) extends BaseHistoricCandleService {

  protected def getCandles(shares: Seq[ShareDTO]): Future[Seq[Seq[HistoricCandle]]] = Future
    .sequence(
      shares.map(s => investApiClient.getCandles(
          figi = s.figi,
          from = appConfig.exchange.startInstantFrom,
          to = appConfig.exchange.startInstantTo,
          interval = CandleInterval.CANDLE_INTERVAL_5_MIN
        ))
    )

  override def getWrappedCandles(shares: Seq[ShareDTO]): Future[Seq[HistoricCandleDTO]] = for {
    candles <- getCandles(shares)
//    _ <- Future { shares.zip(candles).foreach(x => println(x._1.name, x._2.length)) }
    firstCandles <- Future { candles.map(_.headOption) }
    wrappedCandles <- Future { firstCandles.map (c => HistoricCandleDTO(c)) }
  } yield wrappedCandles
}
