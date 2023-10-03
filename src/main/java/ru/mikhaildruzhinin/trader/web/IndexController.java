package ru.mikhaildruzhinin.trader.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequestMapping("/")
public class IndexController {

    @GetMapping
    public String get() {
        return "index";
    }

    @PostMapping
    public String post() {
        log.info("hello world");
        return "index";
    }
}
