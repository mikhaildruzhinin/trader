package com.github.mikhaildruzhinin.trader.database

import slick.jdbc.PostgresProfile.api._

object Connection {
  lazy val db = Database.forConfig("postgres")
}
