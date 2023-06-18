package ru.mikhaildruzhinin.trader.database.connection

import slick.basic.DatabaseConfig
import slick.dbio.DBIO
import slick.jdbc.JdbcProfile

import scala.concurrent.Future

object DatabaseConnection extends Connection {
  override lazy val databaseConfig: DatabaseConfig[JdbcProfile] = DatabaseConfig
    .forConfig[JdbcProfile]("slick")

  override def asyncRun[T](actions: Vector[DBIO[T]]): Future[Vector[T]] = databaseConfig
    .db
    .run(DBIO.sequence(actions))
}
