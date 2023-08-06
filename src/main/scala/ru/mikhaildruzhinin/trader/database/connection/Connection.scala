package ru.mikhaildruzhinin.trader.database.connection

import ru.mikhaildruzhinin.trader.config.AppConfig
import slick.basic.DatabaseConfig
import slick.dbio.DBIO
import slick.jdbc.JdbcProfile

import scala.concurrent.{Await, Future}

trait Connection {
  val databaseConfig: DatabaseConfig[JdbcProfile]

  def asyncRun[T](actions: Vector[DBIO[T]]): Future[Vector[T]] = databaseConfig
    .db
    .run(DBIO.sequence(actions))

  def asyncRun[T](actions: DBIO[T]): Future[T] = databaseConfig
    .db
    .run(actions)

  def runMultiple[T](actions: Vector[DBIO[T]])(implicit appConfig: AppConfig): Vector[T] = Await
    .result(
      asyncRun(actions),
      appConfig.slick.await.duration
    )

  def run[T](action: DBIO[T])(implicit appConfig: AppConfig): Vector[T] = runMultiple(Vector(action))
}
