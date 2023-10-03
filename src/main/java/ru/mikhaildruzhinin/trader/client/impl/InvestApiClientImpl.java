package ru.mikhaildruzhinin.trader.client.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.mikhaildruzhinin.trader.client.InvestApiClient;
import ru.tinkoff.piapi.contract.v1.CandleInterval;
import ru.tinkoff.piapi.contract.v1.HistoricCandle;
import ru.tinkoff.piapi.contract.v1.InstrumentStatus;
import ru.tinkoff.piapi.core.InvestApi;

import java.sql.SQLOutput;
import java.time.Instant;
import java.util.List;


@Component
public class InvestApiClientImpl implements InvestApiClient {

    private final InvestApi investApi;

    public InvestApiClientImpl(@Value("${trader.tinkoffInvestApi.token}") String token) {
        this.investApi = InvestApi.create(token);
    }

    @Override
    public List<HistoricCandle> getCandles(String figi, Instant from, Instant to, CandleInterval interval) {

        return investApi
                .getMarketDataService()
                .getCandlesSync(figi, from, to, interval);
    }
}
