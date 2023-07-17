package ru.mikhaildruzhinin.trader.config

import org.postgresql.ds.PGSimpleDataSource

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.{Duration, FiniteDuration}

case class SlickConfig(profile: String,
                       db: DatabaseConfig,
                       await: AwaitDurationConfig)

case class DatabaseConfig(connectionPool: String,
                          dataSourceClass: String,
                          properties: PropertiesConfig,
                          numThreads: Int = 10)

case class AwaitDurationConfig(length: Int,
                               timeUnit: TimeUnit) {

  val duration: FiniteDuration = Duration(length, timeUnit)
}

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
