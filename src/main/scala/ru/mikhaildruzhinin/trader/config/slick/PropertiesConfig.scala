package ru.mikhaildruzhinin.trader.config.slick

import org.postgresql.ds.PGSimpleDataSource

case class PropertiesConfig(serverName: String,
                            portNumber: Int,
                            databaseName: String,
                            user: String,
                            password: String) {

  val dataSource: PGSimpleDataSource = new PGSimpleDataSource()
  dataSource.setServerNames(Array(serverName))
  dataSource.setPortNumbers(Array(portNumber))
  dataSource.setDatabaseName(databaseName)
  dataSource.setUser(user)
  dataSource.setPassword(password)
}
