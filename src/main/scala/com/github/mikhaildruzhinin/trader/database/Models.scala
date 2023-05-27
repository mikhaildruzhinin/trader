package com.github.mikhaildruzhinin.trader.database

import com.github.mikhaildruzhinin.trader.ShareWrapper
import slick.dbio.Effect
import slick.jdbc.PostgresProfile.api._
import slick.lifted.ProvenShape
import slick.sql.FixedSqlAction

import java.time.Instant

object Models {
  case class Share(id: Long,
                   typeCd: String,
                   figi: String,
                   lot: Int,
                   currency: String,
                   name: String,
                   exchange: String,
                   startingPrice: Option[BigDecimal],
                   purchasePrice: Option[BigDecimal],
                   currentPrice: Option[BigDecimal],
                   updateDttm: Option[Instant],
                   loadDttm: Instant)

  //noinspection MutatorLikeMethodIsParameterless
  class SharesTable(tag: Tag) extends Table[Share](tag, Some("trader"), "shares") {
    def id: Rep[Long] = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def typeCd: Rep[String] = column[String]("type_cd", O.Length(3))
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
      loadDttm
    ) <> (Share.tupled, Share.unapply)
  }

  lazy val sharesTable = TableQuery[SharesTable]
  val ddl = List(sharesTable).map(_.schema).reduce(_ ++ _)

  def insertShares(shares: Seq[ShareWrapper], typeCd: String): FixedSqlAction[Option[Int], NoStream, Effect.Write] = {
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
          s.updateDttm
        )
      ) ++= shares.map(_.getShareTuple(typeCd))
  }
}
