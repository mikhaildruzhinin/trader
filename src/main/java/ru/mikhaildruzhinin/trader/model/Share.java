package ru.mikhaildruzhinin.trader.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@Table(schema = "trader", name = "shares")
public class Share {

    @Id
    private final long id;

    private final String figi;

    private final int lot;

    private final String currency;

    private final String name;

    private final String exchange;

    private final boolean apiTradeAvailableFlag;

    private final boolean buyAvailableFlag;

    private final boolean sellAvailableFlag;

    private final boolean weekendFlag;
}
