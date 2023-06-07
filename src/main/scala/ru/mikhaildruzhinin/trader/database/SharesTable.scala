package ru.mikhaildruzhinin.trader.database

import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.mikhaildruzhinin.trader.database.Connection.databaseConfig.profile.api._
import ru.mikhaildruzhinin.trader.database.Models.{Share, ShareType}
import slick.lifted.ProvenShape

import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalDate, ZoneOffset}
import scala.concurrent.Future

//noinspection MutatorLikeMethodIsParameterless
class SharesTable(tag: Tag) extends Table[Share](tag, Some("trader"), "shares") {
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
    testFlg,
    loadDttm
  ) <> (Share.tupled, Share.unapply)
}

object SharesTable {
  lazy val sharesTable = TableQuery[SharesTable]

  def createIfNotExists: Future[Unit] = Connection.db.run(sharesTable.schema.createIfNotExists)

  def selectAll: Future[Seq[Share]] = Connection.db.run(sharesTable.result)

  def filterByTypeCode(typeCode: Int)
                      (implicit appConfig: AppConfig): Future[Seq[Share]] = {

    val start: Instant = LocalDate
      .now
      .atStartOfDay
      .toInstant(ZoneOffset.UTC)
    val end: Instant = LocalDate
      .now.
      plus(1, ChronoUnit.DAYS)
      .atStartOfDay
      .toInstant(ZoneOffset.UTC)

    val rowNumCol: Rep[Int] = SimpleLiteral[Int](
      "row_number() over(partition by figi, type_cd order by load_dttm desc)"
    )

    val filteredShares: Query[SharesTable, Share, Seq] = sharesTable
      .filter(s =>
        s.updateDttm >= start &&
          s.updateDttm < end &&
          s.testFlg === appConfig.testFlg &&
          s.typeCd === typeCode
      )

    Connection.db.run(
      filteredShares
        .join(
          filteredShares.map(s => (s.id, rowNumCol))
        )
        .on((s, rn) => s.id === rn._1)
        .filter(_._2._2 === 1)
        .map(_._1)
        .sortBy(_.id)
        .result
    )
  }

    def insert(shares: Seq[ShareType]): Future[Option[Int]] = {
      Connection.db.run(
        sharesTable
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
              s.testFlg
            )
          ) ++= shares
      )
  }
}
