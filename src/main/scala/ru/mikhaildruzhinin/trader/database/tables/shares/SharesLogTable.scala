package ru.mikhaildruzhinin.trader.database.tables.shares

import ru.mikhaildruzhinin.trader.database.connection.DatabaseConnection.databaseConfig.profile.api._
import ru.mikhaildruzhinin.trader.database.tables.BaseDao

class SharesLogTable(tag: Tag) extends BaseSharesTable(tag, "shares_log")

object SharesLogTable extends BaseDao[SharesLogTable] with SharesInsertable[SharesLogTable] {
  override protected lazy val table = TableQuery[SharesLogTable]
}
