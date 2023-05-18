package com.github.mikhaildruzhinin.trader

import com.github.kagkarlsson.scheduler.Scheduler
import com.github.kagkarlsson.scheduler.task.helper.{RecurringTask, Tasks}
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import com.typesafe.scalalogging.Logger
import pureconfig.ConfigSource
import pureconfig.generic.auto.exportReader
import ru.tinkoff.piapi.core.{InstrumentsService, InvestApi, MarketDataService}

object TraderScheduler extends App {
  val log: Logger = Logger(getClass.getName.stripSuffix("$"))

  implicit val config: Config = ConfigSource.default.loadOrThrow[Config]

  val token: String = config.tinkoffInvestApi.token
  val api: InvestApi = InvestApi.createSandbox(token)
  implicit val instrumentService: InstrumentsService = api.getInstrumentsService
  implicit val marketDataService: MarketDataService = api.getMarketDataService

  implicit val investApiClient: InvestApiClient.type = InvestApiClient

  val shareWrapper = ShareWrapper

  val uptrendSharesTask: RecurringTask[Void] = Tasks
    .recurring(
      "uptrend-shares",
      Schedules.cron("0 * * * * *") // "0 0 7 * * *"
    )
    .execute((_, _) => {
      shareWrapper
        .getUptrendShares(
          shareWrapper.getShares,
          config.exchange.uptrendCheckTimedeltaHours
        )
        .filter(_.uptrendPct > Some(config.uptrendThresholdPct))
        .toList
        .sortBy(_.uptrendAbs)
        .reverse
        .take(config.numUptrendShares)
        .foreach(s => log.info(s.toString))
    })

  val scheduler: Scheduler = Scheduler
    .create(config.dataSource)
    .startTasks(uptrendSharesTask)
    .threads(5)
    .registerShutdownHook()
    .build()

  scheduler.start()
}
