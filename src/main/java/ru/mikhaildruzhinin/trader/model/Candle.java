package ru.mikhaildruzhinin.trader.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@Table(schema = "trader", name = "candles")
public class Candle {

    @Id
    private final Long id;

    private String figi;

    private final BigDecimal open;

    private final BigDecimal close;

    private final BigDecimal high;

    private final BigDecimal low;

    private final Instant time;
}
