package ru.mikhaildruzhinin.trader.database.tables.base

import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.mikhaildruzhinin.trader.core.TypeCode
import ru.mikhaildruzhinin.trader.core.dto.ShareDTO
import ru.mikhaildruzhinin.trader.database.tables.base.BaseShareDAO.ShareType
import ru.mikhaildruzhinin.trader.database.tables.codegen.{SharesTable, Tables}

import java.sql.Timestamp
import scala.concurrent.Future

trait BaseShareDAO extends SharesTable with Tables {
  def createIfNotExists: Future[Unit]

  def insert(shares: Seq[ShareType]): Future[Option[Int]]

  def selectAll: Future[Seq[SharesRow]]

  def filterByTypeCode(typeCode: Int,
                       testFlg: Boolean): Future[Seq[SharesRow]]

  def update(shares: Seq[ShareDTO],
             typeCode: TypeCode): Future[Seq[Int]]

  def updateTypeCode(figis: Seq[String], typeCode: Int): Future[Int]

  def delete(): Future[Int]

  def toDTO(sharesRow: SharesRow)
           (implicit appConfig: AppConfig): ShareDTO
}

object BaseShareDAO {
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
