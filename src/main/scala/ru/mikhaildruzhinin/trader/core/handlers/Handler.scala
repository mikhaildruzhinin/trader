package ru.mikhaildruzhinin.trader.core.handlers

import com.typesafe.scalalogging.Logger
import ru.mikhaildruzhinin.trader.client.BaseInvestApiClient
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.mikhaildruzhinin.trader.database.connection.Connection

trait Handler {
  val log: Logger = Logger(getClass.getName)

  def apply()(implicit appConfig: AppConfig,
              investApiClient: BaseInvestApiClient,
              connection: Connection): Int
}
