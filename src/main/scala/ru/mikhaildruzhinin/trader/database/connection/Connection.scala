package ru.mikhaildruzhinin.trader.database.connection

import slick.basic.DatabaseConfig
import slick.dbio.DBIO
import slick.jdbc.JdbcProfile

import scala.concurrent.Future

trait Connection {
  val databaseConfig: DatabaseConfig[JdbcProfile]

  def run[T](actions: Vector[DBIO[T]]): Future[Vector[T]]
}
