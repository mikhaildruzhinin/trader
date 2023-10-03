package ru.mikhaildruzhinin.trader.client.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.mikhaildruzhinin.trader.client.InvestApiClient;
import ru.mikhaildruzhinin.trader.model.Candle;
import ru.tinkoff.piapi.contract.v1.CandleInterval;
import ru.tinkoff.piapi.contract.v1.HistoricCandle;
import ru.tinkoff.piapi.core.InvestApi;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static ru.tinkoff.piapi.core.utils.DateUtils.timestampToInstant;
import static ru.tinkoff.piapi.core.utils.MapperUtils.quotationToBigDecimal;


@Component
public class InvestApiClientImpl implements InvestApiClient {

    private final InvestApi investApi;

    public InvestApiClientImpl(@Value("${trader.tinkoffInvestApi.token}") String token) {
        this.investApi = InvestApi.create(token);
    }

    @Override
    public List<Candle> getCandles(String figi, Instant from, Instant to, CandleInterval interval) {

        List<Candle> candles = new ArrayList<>();

        List<HistoricCandle> historicCandles = investApi
                .getMarketDataService()
                .getCandlesSync(figi, from, to, interval)
                .stream().filter(HistoricCandle::getIsComplete)
                .toList();

        for (HistoricCandle historicCandle: historicCandles) {
            BigDecimal open = quotationToBigDecimal(historicCandle.getOpen());
            BigDecimal close = quotationToBigDecimal(historicCandle.getClose());
            BigDecimal high = quotationToBigDecimal(historicCandle.getHigh());
            BigDecimal low = quotationToBigDecimal(historicCandle.getLow());
            Instant time = timestampToInstant(historicCandle.getTime());

            Candle candle = new Candle(open, close, high, low, time);

            candles.add(candle);
        }

        return candles;
    }
}
