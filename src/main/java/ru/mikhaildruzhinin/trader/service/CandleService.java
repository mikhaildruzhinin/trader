package ru.mikhaildruzhinin.trader.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.mikhaildruzhinin.trader.client.InvestApiClient;
import ru.tinkoff.piapi.contract.v1.CandleInterval;
import ru.tinkoff.piapi.contract.v1.HistoricCandle;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CandleService {

    @Autowired
    private InvestApiClient investApiClient;

    public List<HistoricCandle> getCandles(String figi) {
        Instant from = Instant.now().minus(15, ChronoUnit.MINUTES);
        Instant to = Instant.now();
        CandleInterval interval = CandleInterval.CANDLE_INTERVAL_5_MIN;

        List<HistoricCandle> candles = investApiClient
                .getCandles(figi, from, to, interval)
                .stream()
                .filter(HistoricCandle::getIsComplete)
                .collect(Collectors.toList());

        for (HistoricCandle candle: candles) {
            log.info(String.valueOf(candle));
        }
        return candles;
    }

}
