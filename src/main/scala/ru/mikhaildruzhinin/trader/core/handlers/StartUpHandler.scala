package ru.mikhaildruzhinin.trader.core.handlers

import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.mikhaildruzhinin.trader.database.connection.Connection
import ru.mikhaildruzhinin.trader.database.tables.{SharesLogTable, SharesTable}

import scala.concurrent.Await

object StartUpHandler {

  def apply()(implicit appConfig: AppConfig,
              connection: Connection): Unit = {

    Await.result(
      connection.asyncRun(
        Vector(
          SharesTable.createIfNotExists,
          SharesLogTable.createIfNotExists
        )
      ),
      appConfig.slick.await.duration
    )
  }
}
