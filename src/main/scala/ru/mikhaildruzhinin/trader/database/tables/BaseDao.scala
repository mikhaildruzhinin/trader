package ru.mikhaildruzhinin.trader.database.tables

import ru.mikhaildruzhinin.trader.database.connection.DatabaseConnection.databaseConfig.profile.api._

trait BaseDao[T <: Table[_]] {
  import ru.mikhaildruzhinin.trader.database.connection.DatabaseConnection.databaseConfig.profile.ProfileAction

  protected val table: TableQuery[T]

  def createIfNotExists: ProfileAction[Unit, NoStream, Effect.Schema] = table.schema.createIfNotExists
}
