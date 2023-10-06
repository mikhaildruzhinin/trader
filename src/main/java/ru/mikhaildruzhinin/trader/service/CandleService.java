package ru.mikhaildruzhinin.trader.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.mikhaildruzhinin.trader.client.SyncInvestApiClient;
import ru.mikhaildruzhinin.trader.model.Candle;
import ru.mikhaildruzhinin.trader.repository.CandleRepository;
import ru.tinkoff.piapi.contract.v1.CandleInterval;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class CandleService {

    @Autowired
    private SyncInvestApiClient investApiClient;

    @Autowired
    private CandleRepository candleRepository;

    public List<Candle> getCandles(String figi) {
        Instant from = Optional.ofNullable(candleRepository.findMaxTime())
                .orElse(Instant.now().minus(1, ChronoUnit.DAYS));
        Instant to = Instant.now();
        CandleInterval interval = CandleInterval.CANDLE_INTERVAL_5_MIN;

        List<Candle> candles = investApiClient.getCandles(figi, from, to, interval);

        candleRepository.saveAll(candles);

        for (Candle candle: candles) {
            Instant time = candle.getTime();
            log.info(String.valueOf(time));
        }
        return candles;
    }

}
