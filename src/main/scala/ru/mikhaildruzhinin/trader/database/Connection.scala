package ru.mikhaildruzhinin.trader.database

import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

object Connection {
  lazy val databaseConfig = DatabaseConfig.forConfig[JdbcProfile]("slick")
  lazy val db = databaseConfig.db
}
