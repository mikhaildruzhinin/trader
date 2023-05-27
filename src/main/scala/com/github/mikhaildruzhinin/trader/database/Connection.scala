package com.github.mikhaildruzhinin.trader.database

import slick.jdbc.PostgresProfile.api._

object Connection {
  lazy val db = Database.forConfig("postgres")
}

//object Connection {
//  def apply()(implicit appConfig: AppConfig): Connection = new Connection()
//}
