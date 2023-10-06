package ru.mikhaildruzhinin.trader.client.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.mikhaildruzhinin.trader.client.SyncInvestApiClient;
import ru.mikhaildruzhinin.trader.model.Candle;
import ru.mikhaildruzhinin.trader.model.Share;
import ru.tinkoff.piapi.contract.v1.CandleInterval;
import ru.tinkoff.piapi.contract.v1.HistoricCandle;
import ru.tinkoff.piapi.contract.v1.InstrumentStatus;
import ru.tinkoff.piapi.core.InvestApi;

import java.time.Instant;
import java.util.List;

import static ru.tinkoff.piapi.core.utils.DateUtils.timestampToInstant;
import static ru.tinkoff.piapi.core.utils.MapperUtils.quotationToBigDecimal;

@Component
public class SyncInvestApiClientImpl implements SyncInvestApiClient {

    private final InvestApi investApi;

    public SyncInvestApiClientImpl(@Value("${trader.tinkoffInvestApi.token}") String token) {
        this.investApi = InvestApi.create(token);
    }

    @Override
    public List<Candle> getCandles(String figi, Instant from, Instant to, CandleInterval interval) {
        return investApi
                .getMarketDataService()
                .getCandlesSync(figi, from, to, interval)
                .stream().filter(HistoricCandle::getIsComplete)
                .map(historicCandle -> convertCandle(figi, historicCandle))
                .toList();
    }

    private Candle convertCandle(String figi, HistoricCandle historicCandle) {
        return Candle.builder()
                .figi(figi)
                .open(quotationToBigDecimal(historicCandle.getOpen()))
                .close(quotationToBigDecimal(historicCandle.getClose()))
                .high(quotationToBigDecimal(historicCandle.getHigh()))
                .low(quotationToBigDecimal(historicCandle.getLow()))
                .time(timestampToInstant(historicCandle.getTime()))
                .build();
    }

    @Override
    public List<Share> getShares(InstrumentStatus instrumentStatus) {
        return investApi
                .getInstrumentsService()
                .getSharesSync(instrumentStatus)
                .stream()
                .map(this::convertShare)
                .toList();
    }

    private Share convertShare(ru.tinkoff.piapi.contract.v1.Share share) {
        return Share.builder()
                .figi(share.getFigi())
                .lot(share.getLot())
                .currency(share.getCurrency())
                .name(share.getName())
                .exchange(share.getExchange())
                .apiTradeAvailableFlag(share.getApiTradeAvailableFlag())
                .buyAvailableFlag(share.getBuyAvailableFlag())
                .sellAvailableFlag(share.getSellAvailableFlag())
                .weekendFlag(share.getWeekendFlag())
                .build();
    }
}
