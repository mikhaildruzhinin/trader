package ru.mikhaildruzhinin.trader.client;

import ru.tinkoff.piapi.contract.v1.CandleInterval;
import ru.tinkoff.piapi.contract.v1.HistoricCandle;

import java.time.Instant;
import java.util.List;

public interface InvestApiClient {
    List<HistoricCandle> getCandles(String figi, Instant from, Instant to, CandleInterval interval);
}
