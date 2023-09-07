package ru.mikhaildruzhinin.trader.database.tables.impl

import ru.mikhaildruzhinin.trader.config.{AppConfig, TypeCode}
import ru.mikhaildruzhinin.trader.models.ShareModel
import ru.mikhaildruzhinin.trader.database.Connection
import ru.mikhaildruzhinin.trader.database.tables.ShareDAO
import ru.mikhaildruzhinin.trader.database.tables.codegen.{SharesTable, Tables}
import ru.mikhaildruzhinin.trader.database.tables.impl.ShareDAOImpl.InputShareRow
import slick.jdbc.JdbcProfile

import java.sql.Timestamp
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

final class ShareDAOImpl(val connection: Connection)
                        (implicit appConfig: AppConfig)
  extends ShareDAO with SharesTable with Tables {

  override val profile: JdbcProfile = connection.databaseConfig.profile

  import connection.databaseConfig.profile.api._

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

  override def createIfNotExists: Future[Unit] = connection.run(table.schema.createIfNotExists)

  override def insert(shares: Seq[ShareModel],
                      typeCode: TypeCode): Future[Option[Int]] = {
    connection.run(
      table.map(s => (
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
      )) ++= shares.map(_.toInputShareRow(typeCode))
    )
  }

  override def selectAll: Future[Seq[ShareModel]] = connection
    .run(table.result)
    .map(f => f.map(s => toModel(s)))

  override def filterByTypeCode(typeCode: Int,
                                testFlg: Boolean): Future[Seq[ShareModel]] = {

    val (start: Timestamp, end: Timestamp) = getDayInterval

    connection.run(
      table.filter(s =>
        s.loadDttm >= start &&
          s.loadDttm < end &&
          s.testFlg === testFlg &&
          s.typeCd === typeCode.toShort &&
          s.deletedFlg === false
      ).result
    ).map(f => f.map(s => toModel(s)))
  }

  private def updateRow(figi: String,
                        share: InputShareRow,
                        start: Timestamp,
                        end: Timestamp) = table
    .filter(s => {
      s.loadDttm >= start &&
        s.loadDttm < end &&
        s.figi === figi &&
        s.deletedFlg === false
    }).map(s => (
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
    )).update(share)

  override def update(shares: Seq[ShareModel],
                      typeCode: TypeCode): Future[Seq[Int]] = {

    val (start: Timestamp, end: Timestamp) = getDayInterval

    connection.run(
      connection.databaseConfig
        .profile
        .api
        .DBIO
        .sequence(
          shares.map(s => updateRow(s.figi, s.toInputShareRow(typeCode), start, end))
        )
    )
  }

  override def updateTypeCode(figis: Seq[String], typeCode: Int): Future[Int] = {
    val (start: Timestamp, end: Timestamp) = getDayInterval

    connection.run(
      table.filter(s => {
        s.loadDttm >= start &&
          s.loadDttm < end &&
          s.figi.inSet(figis) &&
          s.deletedFlg === false
      }).map(_.typeCd)
        .update(typeCode.toShort)
    )

  }

  override def delete(): Future[Int] = {
    val (start: Timestamp, end: Timestamp) = getDayInterval

    connection.run(
      table.filter(s => {
        s.loadDttm >= start &&
          s.loadDttm < end &&
          s.deletedFlg === false
      }).map(_.deletedFlg)
        .update(true)
    )
  }

  private def toModel(sharesRow: SharesRow): ShareModel = ShareModel
    .builder()
    .fromRowParams(
      sharesRow.figi,
      sharesRow.lot,
      sharesRow.quantity,
      sharesRow.currency,
      sharesRow.name,
      sharesRow.exchange,
      sharesRow.startingPrice,
      sharesRow.purchasePrice,
      sharesRow.currentPrice,
      sharesRow.exchangeUpdateDttm
    ).build()
}

object ShareDAOImpl {
  type InputShareRow = (
    Option[Timestamp],
      Int,
      Option[Int],
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
