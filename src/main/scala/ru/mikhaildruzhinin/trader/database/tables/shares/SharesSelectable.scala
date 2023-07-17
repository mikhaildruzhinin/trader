package ru.mikhaildruzhinin.trader.database.tables.shares

import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.mikhaildruzhinin.trader.database.Models.Share
import ru.mikhaildruzhinin.trader.database.connection.DatabaseConnection.databaseConfig.profile.api._
import ru.mikhaildruzhinin.trader.database.tables.BaseDao

import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalDate, ZoneOffset}

trait SharesSelectable[T <: BaseSharesTable] extends BaseDao[T] {
  import ru.mikhaildruzhinin.trader.database.connection.DatabaseConnection.databaseConfig.profile._

  def selectAll: StreamingProfileAction[Seq[Share], Share, Effect.Read] = table.result

  def filterByTypeCode(typeCode: Int)
                      (implicit appConfig: AppConfig): StreamingProfileAction[Seq[Share], Share, Effect.Read] = {

    val start: Instant = LocalDate
      .now
      .atStartOfDay
      .toInstant(ZoneOffset.UTC)

    val end: Instant = LocalDate
      .now
      .plus(1, ChronoUnit.DAYS)
      .atStartOfDay
      .toInstant(ZoneOffset.UTC)

    table
      .filter(s =>
        s.loadDttm >= start &&
          s.loadDttm < end &&
          s.testFlg === appConfig.testFlg &&
          s.typeCd === typeCode
      )
      .result
  }
}
