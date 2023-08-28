package ru.mikhaildruzhinin.trader.database

import com.typesafe.config.Config
import ru.mikhaildruzhinin.trader.config.AppConfig
import slick.basic.DatabaseConfig
import slick.dbio.DBIO
import slick.jdbc.JdbcProfile

import scala.concurrent.{Await, Future}

trait Connection {
  val databaseConfig: DatabaseConfig[JdbcProfile]

  def run[T](actions: DBIO[T]): Future[T] = databaseConfig
    .db
    .run(actions)
}

object Connection {
  def apply(path: String, config: Config): Connection = new Connection {
    override val databaseConfig: DatabaseConfig[JdbcProfile] = DatabaseConfig
      .forConfig[JdbcProfile](path, config)
  }
}

object DatabaseConnection extends Connection {
  override lazy val databaseConfig: DatabaseConfig[JdbcProfile] = DatabaseConfig
    .forConfig[JdbcProfile]("slick")
}
