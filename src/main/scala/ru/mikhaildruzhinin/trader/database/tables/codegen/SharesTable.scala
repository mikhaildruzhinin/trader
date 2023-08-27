package ru.mikhaildruzhinin.trader.database.tables.codegen
// AUTO-GENERATED Slick data model for table Shares
trait SharesTable {

  self:Tables  =>

  import profile.api._
  import slick.model.ForeignKeyAction
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}
  /** Entity class storing rows of table Shares
   *  @param id Database column id SqlType(bigserial), AutoInc, PrimaryKey
   *  @param exchangeUpdateDttm Database column exchange_update_dttm SqlType(timestamp), Default(None)
   *  @param loadDttm Database column load_dttm SqlType(timestamp)
   *  @param lot Database column lot SqlType(int4)
   *  @param quantity Database column quantity SqlType(int4)
   *  @param typeCd Database column type_cd SqlType(int2)
   *  @param schemaVersion Database column schema_version SqlType(int2), Default(1)
   *  @param testFlg Database column test_flg SqlType(bool)
   *  @param deletedFlg Database column deleted_flg SqlType(bool), Default(false)
   *  @param figi Database column figi SqlType(varchar), Length(12,true)
   *  @param currency Database column currency SqlType(varchar), Length(3,true)
   *  @param name Database column name SqlType(varchar)
   *  @param exchange Database column exchange SqlType(varchar), Length(30,true)
   *  @param startingPrice Database column starting_price SqlType(numeric), Default(None)
   *  @param purchasePrice Database column purchase_price SqlType(numeric), Default(None)
   *  @param currentPrice Database column current_price SqlType(numeric), Default(None)
   *  @param uptrendPct Database column uptrend_pct SqlType(numeric), Default(None)
   *  @param uptrendAbs Database column uptrend_abs SqlType(numeric), Default(None)
   *  @param roi Database column roi SqlType(numeric), Default(None)
   *  @param profit Database column profit SqlType(numeric), Default(None) */
  case class SharesRow(id: Long, exchangeUpdateDttm: Option[java.sql.Timestamp] = None, loadDttm: java.sql.Timestamp, lot: Int, quantity: Int, typeCd: Short, schemaVersion: Short = 1, testFlg: Boolean, deletedFlg: Boolean = false, figi: String, currency: String, name: String, exchange: String, startingPrice: Option[scala.math.BigDecimal] = None, purchasePrice: Option[scala.math.BigDecimal] = None, currentPrice: Option[scala.math.BigDecimal] = None, uptrendPct: Option[scala.math.BigDecimal] = None, uptrendAbs: Option[scala.math.BigDecimal] = None, roi: Option[scala.math.BigDecimal] = None, profit: Option[scala.math.BigDecimal] = None)
  /** GetResult implicit for fetching SharesRow objects using plain SQL queries */
  implicit def GetResultSharesRow(implicit e0: GR[Long], e1: GR[Option[java.sql.Timestamp]], e2: GR[java.sql.Timestamp], e3: GR[Int], e4: GR[Short], e5: GR[Boolean], e6: GR[String], e7: GR[Option[scala.math.BigDecimal]]): GR[SharesRow] = GR{
    prs => import prs._
    SharesRow.tupled((<<[Long], <<?[java.sql.Timestamp], <<[java.sql.Timestamp], <<[Int], <<[Int], <<[Short], <<[Short], <<[Boolean], <<[Boolean], <<[String], <<[String], <<[String], <<[String], <<?[scala.math.BigDecimal], <<?[scala.math.BigDecimal], <<?[scala.math.BigDecimal], <<?[scala.math.BigDecimal], <<?[scala.math.BigDecimal], <<?[scala.math.BigDecimal], <<?[scala.math.BigDecimal]))
  }
  /** Table description of table shares. Objects of this class serve as prototypes for rows in queries. */
  class Shares(_tableTag: Tag) extends profile.api.Table[SharesRow](_tableTag, Some("trader"), "shares") {
    def * = (id, exchangeUpdateDttm, loadDttm, lot, quantity, typeCd, schemaVersion, testFlg, deletedFlg, figi, currency, name, exchange, startingPrice, purchasePrice, currentPrice, uptrendPct, uptrendAbs, roi, profit) <> (SharesRow.tupled, SharesRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), exchangeUpdateDttm, Rep.Some(loadDttm), Rep.Some(lot), Rep.Some(quantity), Rep.Some(typeCd), Rep.Some(schemaVersion), Rep.Some(testFlg), Rep.Some(deletedFlg), Rep.Some(figi), Rep.Some(currency), Rep.Some(name), Rep.Some(exchange), startingPrice, purchasePrice, currentPrice, uptrendPct, uptrendAbs, roi, profit)).shaped.<>({r=>import r._; _1.map(_=> SharesRow.tupled((_1.get, _2, _3.get, _4.get, _5.get, _6.get, _7.get, _8.get, _9.get, _10.get, _11.get, _12.get, _13.get, _14, _15, _16, _17, _18, _19, _20)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(bigserial), AutoInc, PrimaryKey */
    val id: Rep[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)
    /** Database column exchange_update_dttm SqlType(timestamp), Default(None) */
    val exchangeUpdateDttm: Rep[Option[java.sql.Timestamp]] = column[Option[java.sql.Timestamp]]("exchange_update_dttm", O.Default(None))
    /** Database column load_dttm SqlType(timestamp) */
    val loadDttm: Rep[java.sql.Timestamp] = column[java.sql.Timestamp]("load_dttm")
    /** Database column lot SqlType(int4) */
    val lot: Rep[Int] = column[Int]("lot")
    /** Database column quantity SqlType(int4) */
    val quantity: Rep[Int] = column[Int]("quantity")
    /** Database column type_cd SqlType(int2) */
    val typeCd: Rep[Short] = column[Short]("type_cd")
    /** Database column schema_version SqlType(int2), Default(1) */
    val schemaVersion: Rep[Short] = column[Short]("schema_version", O.Default(1))
    /** Database column test_flg SqlType(bool) */
    val testFlg: Rep[Boolean] = column[Boolean]("test_flg")
    /** Database column deleted_flg SqlType(bool), Default(false) */
    val deletedFlg: Rep[Boolean] = column[Boolean]("deleted_flg", O.Default(false))
    /** Database column figi SqlType(varchar), Length(12,true) */
    val figi: Rep[String] = column[String]("figi", O.Length(12,varying=true))
    /** Database column currency SqlType(varchar), Length(3,true) */
    val currency: Rep[String] = column[String]("currency", O.Length(3,varying=true))
    /** Database column name SqlType(varchar) */
    val name: Rep[String] = column[String]("name")
    /** Database column exchange SqlType(varchar), Length(30,true) */
    val exchange: Rep[String] = column[String]("exchange", O.Length(30,varying=true))
    /** Database column starting_price SqlType(numeric), Default(None) */
    val startingPrice: Rep[Option[scala.math.BigDecimal]] = column[Option[scala.math.BigDecimal]]("starting_price", O.Default(None))
    /** Database column purchase_price SqlType(numeric), Default(None) */
    val purchasePrice: Rep[Option[scala.math.BigDecimal]] = column[Option[scala.math.BigDecimal]]("purchase_price", O.Default(None))
    /** Database column current_price SqlType(numeric), Default(None) */
    val currentPrice: Rep[Option[scala.math.BigDecimal]] = column[Option[scala.math.BigDecimal]]("current_price", O.Default(None))
    /** Database column uptrend_pct SqlType(numeric), Default(None) */
    val uptrendPct: Rep[Option[scala.math.BigDecimal]] = column[Option[scala.math.BigDecimal]]("uptrend_pct", O.Default(None))
    /** Database column uptrend_abs SqlType(numeric), Default(None) */
    val uptrendAbs: Rep[Option[scala.math.BigDecimal]] = column[Option[scala.math.BigDecimal]]("uptrend_abs", O.Default(None))
    /** Database column roi SqlType(numeric), Default(None) */
    val roi: Rep[Option[scala.math.BigDecimal]] = column[Option[scala.math.BigDecimal]]("roi", O.Default(None))
    /** Database column profit SqlType(numeric), Default(None) */
    val profit: Rep[Option[scala.math.BigDecimal]] = column[Option[scala.math.BigDecimal]]("profit", O.Default(None))
  }
  /** Collection-like TableQuery object for table Shares */
  lazy val Shares = new TableQuery(tag => new Shares(tag))
}
