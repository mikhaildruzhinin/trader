package ru.mikhaildruzhinin.trader.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.mikhaildruzhinin.trader.client.InvestApiClient;
import ru.mikhaildruzhinin.trader.model.Candle;
import ru.tinkoff.piapi.contract.v1.CandleInterval;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
public class CandleService {

    @Autowired
    private InvestApiClient investApiClient;

    public List<Candle> getCandles(String figi) {
        Instant from = Instant.now().minus(10, ChronoUnit.MINUTES);
        Instant to = Instant.now();
        CandleInterval interval = CandleInterval.CANDLE_INTERVAL_5_MIN;

        List<Candle> candles = investApiClient.getCandles(figi, from, to, interval);


//        candles.get(candles.size() - 1);

        for (Candle candle: candles) {
            Instant time = candle.getTime();
            log.info(String.valueOf(time));
        }
        return candles;
    }

}
