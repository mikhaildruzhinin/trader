package ru.mikhaildruzhinin.trader.config.slick

case class DatabaseConfig(connectionPool: String,
                          dataSourceClass: String,
                          properties: PropertiesConfig,
                          numThreads: Int = 10)
