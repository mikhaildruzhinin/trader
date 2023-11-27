package ru.mikhaildruzhinin.trader

import ru.tinkoff.piapi.contract.v1.CandleInterval

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait ShareService {
  def getShares(client: InvestApiClient,
                appConfig: AppConfig,
                candleInterval: CandleInterval): Future[List[Share]]
}

object ShareServiceImpl extends ShareService {
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
