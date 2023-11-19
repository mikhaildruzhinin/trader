package ru.mikhaildruzhinin.trader

case class AppConfig(tinkoffInvestApiToken: String,
                     exchanges: List[String])
