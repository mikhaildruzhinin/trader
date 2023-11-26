package ru.mikhaildruzhinin.trader

import com.google.protobuf.Timestamp
import ru.tinkoff.piapi.contract.v1.{CandleInterval, Share => TinkoffShare}

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class Share private (figi: String,
                          ticker: String,
                          classCode: String,
                          isin: String,
                          lot: Int,
                          currency: String,
                          name: String,
                          exchange: String,
                          uid: String,
                          forQualInvestorFlag: Boolean,
                          weekendFlag: Boolean,
                          first1MinCandleDate: Timestamp,
                          first1DayCandleDate: Timestamp,
                          candles: Option[List[Candle]] = None)

trait ShareService {
  def getShares(client: InvestApiClient,
                appConfig: AppConfig,
                candleInterval: CandleInterval): Future[List[Share]]
}

object Share extends ShareService {
  def apply(tinkoffShare: TinkoffShare): Share = Share(
    tinkoffShare.getFigi,
    ticker = tinkoffShare.getTicker,
    classCode = tinkoffShare.getClassCode,
    isin = tinkoffShare.getIsin,
    lot = tinkoffShare.getLot,
    currency = tinkoffShare.getCurrency,
    name = tinkoffShare.getName,
    exchange = tinkoffShare.getExchange,
    uid = tinkoffShare.getUid,
    forQualInvestorFlag = tinkoffShare.getForQualInvestorFlag,
    weekendFlag = tinkoffShare.getWeekendFlag,
    first1MinCandleDate = tinkoffShare.getFirst1MinCandleDate,
    first1DayCandleDate = tinkoffShare.getFirst1DayCandleDate
  )

  override def getShares(client: InvestApiClient,
                         appConfig: AppConfig,
                         candleInterval: CandleInterval): Future[List[Share]] = for {

    shares <- client.getShares()

    filteredShares <- Future {
      shares.filter(share => appConfig.exchanges.contains(share.exchange))
    }

    candles <- Future.sequence {
      Thread.sleep(1000L)
      filteredShares.map(share => client
        .getCandles(
          share = share,
          from = Instant.now().minus(1, ChronoUnit.DAYS),
          to = Instant.now(),
          candleInterval = candleInterval
        )
      )
    }

    sharesWithCandles <- Future {
      filteredShares.zip(candles).map(sc => sc._1.copy(candles = Some(sc._2)))
    }
  } yield sharesWithCandles
}
