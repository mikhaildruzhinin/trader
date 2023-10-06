package ru.mikhaildruzhinin.trader.client;

import ru.mikhaildruzhinin.trader.model.Candle;
import ru.mikhaildruzhinin.trader.model.Share;
import ru.tinkoff.piapi.contract.v1.CandleInterval;
import ru.tinkoff.piapi.contract.v1.InstrumentStatus;

import java.time.Instant;
import java.util.List;

public interface SyncInvestApiClient {
    List<Candle> getCandles(String figi, Instant from, Instant to, CandleInterval interval);

    List<Share> getShares(InstrumentStatus instrumentStatus);
}
