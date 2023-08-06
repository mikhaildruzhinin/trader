package ru.mikhaildruzhinin.trader

import com.github.kagkarlsson.scheduler.task.helper.{OneTimeTask, RecurringTask, Tasks}
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import com.typesafe.scalalogging.Logger
import pureconfig._
import pureconfig.generic.ProductHint
import pureconfig.generic.auto.exportReader
import pureconfig.generic.semiauto.deriveEnumerationReader
import ru.mikhaildruzhinin.trader.client.{BaseInvestApiClient, ResilientInvestApiClient}
import ru.mikhaildruzhinin.trader.config.{AppConfig, InvestApiMode}
import ru.mikhaildruzhinin.trader.core.handlers._
import ru.mikhaildruzhinin.trader.core.services.AvailabilityService
import ru.mikhaildruzhinin.trader.database.connection.{Connection, DatabaseConnection}
import ru.tinkoff.piapi.core.InvestApi

import java.time.ZoneId
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

trait Components {
  val log: Logger = Logger(getClass.getName)

  private implicit def hint[A]: ProductHint[A] = ProductHint[A](ConfigFieldMapping(CamelCase, CamelCase))
  private implicit lazy val investApiModeConvert: ConfigReader[InvestApiMode] = deriveEnumerationReader[InvestApiMode]
  implicit lazy val appConfig: AppConfig = ConfigSource.default.loadOrThrow[AppConfig]

  lazy val investApi: InvestApi = appConfig.tinkoffInvestApi.mode match {
    case InvestApiMode.Trade => InvestApi.create(appConfig.tinkoffInvestApi.token)
    case InvestApiMode.Readonly => InvestApi.createReadonly(appConfig.tinkoffInvestApi.token)
    case InvestApiMode.Sandbox => InvestApi.createSandbox(appConfig.tinkoffInvestApi.token)
  }

  implicit lazy val investApiClient: BaseInvestApiClient = ResilientInvestApiClient(investApi)

  implicit lazy val connection: Connection = DatabaseConnection

  private val availabilityService = new AvailabilityService()

  val startUpTask: OneTimeTask[Void] = Tasks
    .oneTime("start-up")
    .execute((_, _) => StartUpHandler())

  val purchaseTask: RecurringTask[Void] = Tasks
    .recurring(
      "purchase",
      Schedules.cron(
        "0 10 9 * * *",
        ZoneId.of("UTC")
      )
    ).execute((_, _) => {
    val availableShares: Future[Option[Int]] = availabilityService
      .getAvailableShares

    availableShares.onComplete {
      case Success(s) => log.info(s"Total: ${s.getOrElse(0)}")
      case Failure(exception) => exception.printStackTrace()
    }
    UptrendHandler()
    PurchaseHandler()
  })

  val monitorTask: RecurringTask[Void] = Tasks
    .recurring(
      "monitor",
      Schedules.cron(
        "0 12/2 9-12 * * *",
        ZoneId.of("UTC")
      )
    ).execute((_, _) => MonitorHandler())

  val sellTask: RecurringTask[Void] = Tasks
    .recurring(
      "sell",
      Schedules.cron(
        "0 0 13 * * *",
        ZoneId.of("UTC")
      )
    ).execute((_, _) => SellHandler())
}
