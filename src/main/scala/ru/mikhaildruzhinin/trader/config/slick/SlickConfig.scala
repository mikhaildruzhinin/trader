package ru.mikhaildruzhinin.trader.config.slick

case class SlickConfig(profile: String,
                       db: DatabaseConfig,
                       await: AwaitDurationConfig)
