package ru.mikhaildruzhinin.trader.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@AllArgsConstructor
public class Candle {

    private final BigDecimal open;

    private final BigDecimal close;

    private final BigDecimal high;

    private final BigDecimal low;

    private final Instant time;
}
