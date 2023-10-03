package ru.mikhaildruzhinin.trader.client;

import ru.mikhaildruzhinin.trader.model.Candle;
import ru.tinkoff.piapi.contract.v1.CandleInterval;

import java.time.Instant;
import java.util.List;

public interface InvestApiClient {
    List<Candle> getCandles(String figi, Instant from, Instant to, CandleInterval interval);
}
