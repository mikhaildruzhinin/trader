package ru.mikhaildruzhinin.trader.database.tables

import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.mikhaildruzhinin.trader.core.dto.ShareDTO
import ru.mikhaildruzhinin.trader.database.tables.ShareDAO.ShareType
import ru.mikhaildruzhinin.trader.database.tables.codegen.{SharesTable, Tables}
import slick.jdbc.JdbcProfile
import slick.sql.{FixedSqlAction, FixedSqlStreamingAction}

import java.sql.Timestamp
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class ShareDAO(val profile: JdbcProfile) extends SharesTable with Tables {
  import profile.api._

  //noinspection ScalaWeakerAccess
  class S(_tableTag: Tag) extends Shares(_tableTag) {
    override val loadDttm: Rep[java.sql.Timestamp] = column[java.sql.Timestamp](
      "load_dttm",
      O.SqlType("timestamp default now()")
    )
  }

  private lazy val table = TableQuery[S]

  private def getDayInterval: (Timestamp, Timestamp) = {
    val start: Timestamp = Timestamp.valueOf(LocalDate.now.atStartOfDay)

    val end: Timestamp = Timestamp.valueOf(
      LocalDate.now.plus(1, ChronoUnit.DAYS).atStartOfDay
    )

    (start, end)
  }

  def createIfNotExists: FixedSqlAction[Unit, NoStream, Effect.Schema] = table.schema.createIfNotExists

  def insert(shares: Seq[ShareType]): FixedSqlAction[Option[Int], NoStream, Effect.Write] = {
    table
      .map(
        s => (
          s.exchangeUpdateDttm,
          s.lot,
          s.quantity,
          s.typeCd,
          s.testFlg,
          s.figi,
          s.currency,
          s.name,
          s.exchange,
          s.startingPrice,
          s.purchasePrice,
          s.currentPrice,
          s.uptrendPct,
          s.uptrendAbs,
          s.roi,
          s.profit
        )
      ) ++= shares
  }

  def selectAll: FixedSqlStreamingAction[Seq[SharesRow], SharesRow, Effect.Read] = table.result

  def filterByTypeCode(typeCode: Int,
                       testFlg: Boolean): FixedSqlStreamingAction[Seq[SharesRow], SharesRow, Effect.Read] = {

    val (start: Timestamp, end: Timestamp) = getDayInterval

    table
      .filter(s =>
        s.loadDttm >= start &&
          s.loadDttm < end &&
          s.testFlg === testFlg &&
          s.typeCd === typeCode.toShort &&
          s.deletedFlg === false
      )
      .result
  }

  def update(figi: String,
             share: ShareType): FixedSqlAction[Int, NoStream, Effect.Write] = {

    val (start: Timestamp, end: Timestamp) = getDayInterval

    table
      .filter(s => {
        s.loadDttm >= start &&
          s.loadDttm < end &&
          s.figi === figi &&
          s.deletedFlg === false
      })
      .map(
        s => (
          s.exchangeUpdateDttm,
          s.lot,
          s.quantity,
          s.typeCd,
          s.testFlg,
          s.figi,
          s.currency,
          s.name,
          s.exchange,
          s.startingPrice,
          s.purchasePrice,
          s.currentPrice,
          s.uptrendPct,
          s.uptrendAbs,
          s.roi,
          s.profit
        )
      )
      .update(share)
  }

  def updateTypeCode(figis: Seq[String], typeCode: Int): FixedSqlAction[Int, NoStream, Effect.Write] = {
    val (start: Timestamp, end: Timestamp) = getDayInterval

    table
      .filter(s => {
        s.loadDttm >= start &&
          s.loadDttm < end &&
          s.figi.inSet(figis) &&
          s.deletedFlg === false
      })
      .map(_.typeCd)
      .update(typeCode.toShort)
  }

  def delete(): FixedSqlAction[Int, NoStream, Effect.Write] = {
    val (start: Timestamp, end: Timestamp) = getDayInterval

    table
      .filter(s => {
        s.loadDttm >= start &&
          s.loadDttm < end &&
          s.deletedFlg === false
      })
      .map(_.deletedFlg)
      .update(true)
  }

  def toDTO(sharesRow: SharesRow)
           (implicit appConfig: AppConfig): ShareDTO = ShareDTO
    .builder()
    .fromRowParams(
      sharesRow.figi,
      sharesRow.lot,
      sharesRow.currency,
      sharesRow.name,
      sharesRow.exchange,
      sharesRow.startingPrice,
      sharesRow.purchasePrice,
      sharesRow.currentPrice,
      sharesRow.exchangeUpdateDttm
    ).build()
}

object ShareDAO {
  type ShareType = (
    Option[Timestamp],
      Int,
      Int,
      Short,
      Boolean,
      String,
      String,
      String,
      String,
      Option[BigDecimal],
      Option[BigDecimal],
      Option[BigDecimal],
      Option[BigDecimal],
      Option[BigDecimal],
      Option[BigDecimal],
      Option[BigDecimal]
    )
}
