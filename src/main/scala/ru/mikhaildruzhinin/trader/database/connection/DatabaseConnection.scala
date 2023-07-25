package ru.mikhaildruzhinin.trader.database.connection

import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

object DatabaseConnection extends Connection {
  override lazy val databaseConfig: DatabaseConfig[JdbcProfile] = DatabaseConfig
    .forConfig[JdbcProfile]("slick")
}
