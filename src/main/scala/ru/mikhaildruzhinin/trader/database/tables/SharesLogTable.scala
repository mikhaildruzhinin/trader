package ru.mikhaildruzhinin.trader.database.tables

import ru.mikhaildruzhinin.trader.database.Models.{Share, ShareType}
import ru.mikhaildruzhinin.trader.database.connection.DatabaseConnection.databaseConfig.profile.api._
import slick.lifted.ProvenShape

import java.time.Instant

//noinspection MutatorLikeMethodIsParameterless
class SharesLogTable(tag: Tag) extends Table[Share](tag, Some("trader"), "shares_log") {
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

  def loadDttm: Rep[Instant] = column[Instant](
    "load_dttm",
    O.SqlType("timestamp default now()")
  )

  override def * : ProvenShape[Share] = (
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
    loadDttm
  ) <> (Share.tupled, Share.unapply)
}

object SharesLogTable {
  import ru.mikhaildruzhinin.trader.database.connection.DatabaseConnection.databaseConfig.profile.ProfileAction

  private lazy val table = TableQuery[SharesLogTable]

  def createIfNotExists: ProfileAction[Unit, NoStream, Effect.Schema] = table.schema.createIfNotExists

  def insert(shares: Seq[ShareType]): ProfileAction[Option[Int], NoStream, Effect.Write] = {
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
}
