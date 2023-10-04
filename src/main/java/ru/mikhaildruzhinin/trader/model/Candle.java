package ru.mikhaildruzhinin.trader.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class Candle {

    private final Long id;

    private String figi;

    private final BigDecimal open;

    private final BigDecimal close;

    private final BigDecimal high;

    private final BigDecimal low;

    private final Instant time;
}
