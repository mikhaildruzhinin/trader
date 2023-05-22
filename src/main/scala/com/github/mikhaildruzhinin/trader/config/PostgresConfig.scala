package com.github.mikhaildruzhinin.trader.config

import org.postgresql.ds.PGSimpleDataSource

case class PostgresConfig(host: String,
                          port: Int,
                          db: String,
                          user: String,
                          password: String) {

  val dataSource: PGSimpleDataSource = new PGSimpleDataSource()
  dataSource.setServerNames(Array(host))
  dataSource.setPortNumbers(Array(port))
  dataSource.setDatabaseName(db)
  dataSource.setUser(user)
  dataSource.setPassword(password)
}
