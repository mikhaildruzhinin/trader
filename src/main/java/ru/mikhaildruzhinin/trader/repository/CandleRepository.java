package ru.mikhaildruzhinin.trader.repository;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import ru.mikhaildruzhinin.trader.model.Candle;

import java.time.Instant;

public interface CandleRepository extends CrudRepository<Candle, Long> {
    @Query("select time from trader.candles order by time desc limit 1")
    public Instant findMaxTime();
}
