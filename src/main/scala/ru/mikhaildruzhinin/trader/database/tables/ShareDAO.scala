package ru.mikhaildruzhinin.trader.database.tables

import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.mikhaildruzhinin.trader.core.wrappers.ShareWrapper
import ru.mikhaildruzhinin.trader.database.tables.ShareDAO.ShareType
import ru.mikhaildruzhinin.trader.database.tables.codegen.{SharesTable, Tables}
import slick.jdbc.JdbcProfile

import java.sql.Timestamp
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class ShareDAO(val profile: JdbcProfile) extends SharesTable with Tables {
  import profile.api._

  class SS(_tableTag: Tag) extends Shares(_tableTag) {
    override val loadDttm: Rep[java.sql.Timestamp] = column[java.sql.Timestamp](
      "load_dttm",
      O.SqlType("timestamp default now()")
    )
  }

  private lazy val table = TableQuery[SS]

  private def getDayInterval: (Timestamp, Timestamp) = {
    val start: Timestamp = Timestamp.valueOf(LocalDate.now.atStartOfDay)
//      .toInstant(ZoneOffset.UTC)

    val end: Timestamp = Timestamp.valueOf(
      LocalDate.now.plus(1, ChronoUnit.DAYS).atStartOfDay
    )

    (start, end)
  }

  def createIfNotExists = table.schema.createIfNotExists

  def insert(shares: Seq[ShareType]) = {
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

  def selectAll = table.result

  def filterByTypeCode(typeCode: Int)
                      (implicit appConfig: AppConfig) = {

    val (start: Timestamp, end: Timestamp) = getDayInterval

    table
      .filter(s =>
        s.loadDttm >= start &&
          s.loadDttm < end &&
          s.testFlg === appConfig.testFlg &&
          s.typeCd === typeCode.toShort &&
          s.deletedFlg === false
      )
      .result
  }

  def update(figi: String,
             share: ShareType) = {

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

  def updateTypeCode(figis: Seq[String], typeCode: Int) = {
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

  def delete() = {
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
           (implicit appConfig: AppConfig): ShareWrapper = ShareWrapper
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
    Option[java.sql.Timestamp],
      Int,
      Int,
      Short,
      Boolean,
      String,
      String,
      String,
      String,
      Option[scala.math.BigDecimal],
      Option[scala.math.BigDecimal],
      Option[scala.math.BigDecimal],
      Option[scala.math.BigDecimal],
      Option[scala.math.BigDecimal],
      Option[scala.math.BigDecimal],
      Option[scala.math.BigDecimal]
    )
}
