package ru.mikhaildruzhinin.trader.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.mikhaildruzhinin.trader.service.CandleService;

@Slf4j
@Controller
@RequestMapping("/")
public class IndexController {

    @Autowired
    private CandleService candleService;

    @GetMapping
    public String get() {
        return "index";
    }

    @PostMapping
    public String post(@RequestParam("figi") String figi) {
        log.info(figi);

        candleService.getCandles(figi);
        return "index";
    }
}
