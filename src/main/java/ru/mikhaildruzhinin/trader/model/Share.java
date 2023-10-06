package ru.mikhaildruzhinin.trader.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
@Builder
public class Share {

    @Id
    private final long id;

    private final String figi;
}
