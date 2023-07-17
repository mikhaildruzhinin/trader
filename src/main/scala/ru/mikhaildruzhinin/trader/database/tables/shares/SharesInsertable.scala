package ru.mikhaildruzhinin.trader.database.tables.shares

import ru.mikhaildruzhinin.trader.database.Models.ShareType
import ru.mikhaildruzhinin.trader.database.connection.DatabaseConnection.databaseConfig.profile.api._
import ru.mikhaildruzhinin.trader.database.tables.BaseDao

trait SharesInsertable[T <: BaseSharesTable] extends BaseDao[T] {
  import ru.mikhaildruzhinin.trader.database.connection.DatabaseConnection.databaseConfig.profile._

  def insert(shares: Seq[ShareType]): ProfileAction[Option[Int], NoStream, Effect.Write] = {
    table
      .map(
        s => (
          s.typeCd,
          s.figi,
          s.lot,
          s.currency,
          s.name,
          s.exchange,
          s.startingPrice,
          s.purchasePrice,
          s.currentPrice,
          s.updateDttm,
          s.uptrendPct,
          s.uptrendAbs,
          s.roi,
          s.profit,
          s.testFlg
        )
      ) ++= shares
  }
}
