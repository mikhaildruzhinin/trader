package ru.mikhaildruzhinin.trader.database.tables

import ru.mikhaildruzhinin.trader.TypeCode
import ru.mikhaildruzhinin.trader.models.ShareModel

import scala.concurrent.Future

trait ShareDAO  {
  def createIfNotExists: Future[Unit]

  def insert(shares: Seq[ShareModel],
             typeCode: TypeCode): Future[Option[Int]]

  def selectAll: Future[Seq[ShareModel]]

  def filterByTypeCode(typeCode: Int,
                       testFlg: Boolean): Future[Seq[ShareModel]]

  def update(shares: Seq[ShareModel],
             typeCode: TypeCode): Future[Seq[Int]]

  def updateTypeCode(figis: Seq[String], typeCode: Int): Future[Int]

  def delete(): Future[Int]
}
