package ru.mikhaildruzhinin.trader.database.tables.shares

import ru.mikhaildruzhinin.trader.database.connection.DatabaseConnection.databaseConfig.profile.api._
import ru.mikhaildruzhinin.trader.database.tables.BaseDao

class SharesOperationsTable(tag: Tag) extends BaseSharesTable(tag, "shares_operations")

object SharesOperationsTable extends BaseDao[SharesOperationsTable]
  with SharesInsertable[SharesOperationsTable] with SharesSelectable[SharesOperationsTable] {

  override protected lazy val table = TableQuery[SharesOperationsTable]
}
