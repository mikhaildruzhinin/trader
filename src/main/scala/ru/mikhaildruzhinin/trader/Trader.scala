package ru.mikhaildruzhinin.trader

import org.flywaydb.core.Flyway

object Trader extends App with Components {
  Flyway.configure()
    .dataSource(appConfig.slick.db.properties.dataSource)
    .schemas("trader")
    .load()
    .migrate()

  scheduler.start()
}
