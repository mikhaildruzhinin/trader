package ru.mikhaildruzhinin.trader.database.tables

import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.mikhaildruzhinin.trader.database.Models.{ShareModel, ShareType}
import slick.jdbc.JdbcProfile
import slick.lifted.ProvenShape

import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalDate, ZoneOffset}

class ShareDAO(val profile: JdbcProfile) {
  import profile.api._

  //noinspection MutatorLikeMethodIsParameterless,ScalaWeakerAccess
  private class SharesTable(tag: Tag) extends Table[ShareModel](tag, Some("trader"), "shares") {
    def id: Rep[Long] = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def typeCd: Rep[Int] = column[Int]("type_cd")

    def figi: Rep[String] = column[String]("figi")

    def lot: Rep[Int] = column[Int]("lot")

    def currency: Rep[String] = column[String]("currency", O.Length(3))

    def name: Rep[String] = column[String]("name")

    def exchange: Rep[String] = column[String]("exchange")

    def startingPrice: Rep[Option[BigDecimal]] = column[Option[BigDecimal]](
      "starting_price",
      O.SqlType("decimal(15,9)")
    )

    def purchasePrice: Rep[Option[BigDecimal]] = column[Option[BigDecimal]](
      "purchase_price",
      O.SqlType("decimal(15,9)")
    )

    def currentPrice: Rep[Option[BigDecimal]] = column[Option[BigDecimal]](
      "current_price",
      O.SqlType("decimal(15,9)")
    )

    def updateDttm: Rep[Option[Instant]] = column[Option[Instant]](
      "update_dttm",
      O.SqlType("timestamp")
    )

    def uptrendPct: Rep[Option[BigDecimal]] = column[Option[BigDecimal]]("uptrend_pct")

    def uptrendAbs: Rep[Option[BigDecimal]] = column[Option[BigDecimal]]("uptrend_abs")

    def roi: Rep[Option[BigDecimal]] = column[Option[BigDecimal]]("roi")

    def profit: Rep[Option[BigDecimal]] = column[Option[BigDecimal]]("profit")

    def testFlg: Rep[Boolean] = column[Boolean]("test_flg")

    def deletedFlg: Rep[Boolean] = column[Boolean]("deleted_flg", O.Default(false))

    def loadDttm: Rep[Instant] = column[Instant](
      "load_dttm",
      O.SqlType("timestamp default now()")
    )

    override def * : ProvenShape[ShareModel] = (
      id,
      typeCd,
      figi,
      lot,
      currency,
      name,
      exchange,
      startingPrice,
      purchasePrice,
      currentPrice,
      updateDttm,
      uptrendPct,
      uptrendAbs,
      roi,
      profit,
      testFlg,
      deletedFlg,
      loadDttm
    ) <> (ShareModel.tupled, ShareModel.unapply)
  }

  private lazy val table = TableQuery[SharesTable]

  private def getDayInterval: (Instant, Instant) = {
    val start: Instant = LocalDate
      .now
      .atStartOfDay
      .toInstant(ZoneOffset.UTC)

    val end: Instant = LocalDate
      .now
      .plus(1, ChronoUnit.DAYS)
      .atStartOfDay
      .toInstant(ZoneOffset.UTC)

    (start, end)
  }

  def createIfNotExists: profile.ProfileAction[Unit, NoStream, Effect.Schema] = table.schema.createIfNotExists

  def insert(shares: Seq[ShareType]): profile.ProfileAction[Option[Int], NoStream, Effect.Write] = {
    table
      .map(
        s => (
          s.typeCd,
          s.figi,
          s.lot,
          s.currency,
          s.name,
          s.exchange,
          s.startingPrice,
          s.purchasePrice,
          s.currentPrice,
          s.updateDttm,
          s.uptrendPct,
          s.uptrendAbs,
          s.roi,
          s.profit,
          s.testFlg
        )
      ) ++= shares
  }

  def selectAll: profile.StreamingProfileAction[Seq[ShareModel], ShareModel, Effect.Read] = table.result

  def filterByTypeCode(typeCode: Int)
                      (implicit appConfig: AppConfig): profile.StreamingProfileAction[Seq[ShareModel], ShareModel, Effect.Read] = {

    val (start: Instant, end: Instant) = getDayInterval

    table
      .filter(s =>
        s.loadDttm >= start &&
          s.loadDttm < end &&
          s.testFlg === appConfig.testFlg &&
          s.typeCd === typeCode &&
          s.deletedFlg === false
      )
      .result
  }

  def update(figi: String,
             share: ShareType): profile.ProfileAction[Int, NoStream, Effect.Write] = {

    val (start: Instant, end: Instant) = getDayInterval

    table
      .filter(s => {
        s.loadDttm >= start &&
          s.loadDttm < end &&
          s.figi === figi &&
          s.deletedFlg === false
      })
      .map(
        s => (
          s.typeCd,
          s.figi,
          s.lot,
          s.currency,
          s.name,
          s.exchange,
          s.startingPrice,
          s.purchasePrice,
          s.currentPrice,
          s.updateDttm,
          s.uptrendPct,
          s.uptrendAbs,
          s.roi,
          s.profit,
          s.testFlg
        )
      )
      .update(share)
  }

  def updateTypeCode(figis: Seq[String], typeCode: Int): profile.ProfileAction[Int, NoStream, Effect.Write] = {
    val (start: Instant, end: Instant) = getDayInterval

    table
      .filter(s => {
        s.loadDttm >= start &&
          s.loadDttm < end &&
          s.figi.inSet(figis) &&
          s.deletedFlg === false
      })
      .map(_.typeCd)
      .update(typeCode)
  }

  def delete(): profile.ProfileAction[Int, NoStream, Effect.Write] = {
    val (start: Instant, end: Instant) = getDayInterval

    table
      .filter(s => {
        s.loadDttm >= start &&
          s.loadDttm < end &&
          s.deletedFlg === false
      })
      .map(_.deletedFlg)
      .update(true)
  }
}
