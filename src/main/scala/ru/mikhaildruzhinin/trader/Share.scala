package ru.mikhaildruzhinin.trader

import com.google.protobuf.Timestamp
import ru.tinkoff.piapi.contract.v1.{Share => TinkoffShare}

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

object Share {
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
}
