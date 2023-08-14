package ru.mikhaildruzhinin.trader

import com.github.kagkarlsson.scheduler.task.helper.{OneTimeTask, RecurringTask, Tasks}
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import com.typesafe.scalalogging.Logger
import pureconfig.generic.ProductHint
import pureconfig.generic.auto.exportReader
import pureconfig.generic.semiauto.deriveEnumerationReader
import pureconfig.{CamelCase, ConfigFieldMapping, ConfigReader, ConfigSource}
import ru.mikhaildruzhinin.trader.client.{BaseInvestApiClient, ResilientInvestApiClient}
import ru.mikhaildruzhinin.trader.config.{AppConfig, InvestApiMode}
import ru.mikhaildruzhinin.trader.core.TypeCode.{Available, Uptrend}
import ru.mikhaildruzhinin.trader.core.handlers._
import ru.mikhaildruzhinin.trader.core.services.base._
import ru.mikhaildruzhinin.trader.core.services.impl._
import ru.mikhaildruzhinin.trader.database.connection.{Connection, DatabaseConnection}
import ru.tinkoff.piapi.core.InvestApi

import java.time.ZoneId
import java.util.concurrent.TimeUnit
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

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

  private val shareService: BaseShareService = new ShareService(investApiClient, connection)
  private val historicCandleService: BaseHistoricCandleService = new HistoricCandleService(investApiClient, connection)
  private val priceService: BasePriceService = new PriceService(investApiClient, connection)

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
      val result = for {
        shares <- shareService.getAvailableShares
        candles <- historicCandleService.getWrappedCandles(shares)
        updatedShares <- shareService.getUpdatedShares(shares, candles)
        numUpdatedShares <- shareService.persistNewShares(updatedShares, Available)
        _ <- Future {
          log.info(s"Total: ${numUpdatedShares.getOrElse(0)}")
        }
        persistedShares <- shareService.getPersistedShares(Available)
        currentPrices <- priceService.getCurrentPrices(persistedShares)
        updatedShares <- shareService.updatePrices(persistedShares, currentPrices)
        uptrendShares <- shareService.filterUptrend(updatedShares)
        numUptrendShares <- shareService.persistUpdatedShares(uptrendShares, Uptrend)
        _ <- Future {
          log.info(s"Best uptrend: ${numUptrendShares.sum}")
        }
      } yield numUptrendShares

    Await.result(result, Duration(10, TimeUnit.SECONDS))

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
