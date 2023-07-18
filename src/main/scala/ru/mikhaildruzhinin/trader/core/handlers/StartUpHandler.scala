package ru.mikhaildruzhinin.trader.core.handlers

import ru.mikhaildruzhinin.trader.client.BaseInvestApiClient
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.mikhaildruzhinin.trader.database.connection.Connection
import ru.mikhaildruzhinin.trader.database.tables.SharesTable

import scala.concurrent.Await

object StartUpHandler extends Handler {

  override def apply()(implicit appConfig: AppConfig,
                       investApiClient: BaseInvestApiClient,
                       connection: Connection): Int = {

    Await.result(
      connection.asyncRun(
        Vector(
          SharesTable.createIfNotExists,
        )
      ),
      appConfig.slick.await.duration
    )
    0 // returns OK status
  }
}
