package ru.mikhaildruzhinin.trader

import com.typesafe.scalalogging.Logger
import ru.mikhaildruzhinin.trader.client.{BaseInvestApiClient, SyncInvestApiClient}
import ru.mikhaildruzhinin.trader.config.{AppConfig, ConfigReader}
import ru.mikhaildruzhinin.trader.core.handlers._
import ru.mikhaildruzhinin.trader.database.connection.{Connection, DatabaseConnection}

object Main extends App {

  val log: Logger = Logger(getClass.getName)
  log.info("Start")

  implicit val appConfig: AppConfig = ConfigReader.appConfig
  implicit val connection: Connection = DatabaseConnection
  implicit val investApiClient: BaseInvestApiClient = SyncInvestApiClient

  StartUpHandler()
  PurchaseHandler()
  MonitorHandler()
  SellHandler()
}
