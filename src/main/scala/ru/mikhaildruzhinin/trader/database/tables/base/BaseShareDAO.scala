package ru.mikhaildruzhinin.trader.database.tables.base

import ru.mikhaildruzhinin.trader.core.TypeCode
import ru.mikhaildruzhinin.trader.core.dto.ShareDTO

import java.sql.Timestamp
import scala.concurrent.Future

trait BaseShareDAO  {
  def createIfNotExists: Future[Unit]

  def insert(shares: Seq[ShareDTO],
             typeCode: TypeCode): Future[Option[Int]]

  def selectAll: Future[Seq[ShareDTO]]

  def filterByTypeCode(typeCode: Int,
                       testFlg: Boolean): Future[Seq[ShareDTO]]

  def update(shares: Seq[ShareDTO],
             typeCode: TypeCode): Future[Seq[Int]]

  def updateTypeCode(figis: Seq[String], typeCode: Int): Future[Int]

  def delete(): Future[Int]
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
