package com.github.mikhaildruzhinin.trader

import com.github.mikhaildruzhinin.trader.config.AppConfig
import com.github.mikhaildruzhinin.trader.database.SharesTable

import java.util.concurrent.TimeUnit
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Util {
  def getShares(typeCd: Int)
               (implicit appConfig: AppConfig): Seq[ShareWrapper] = {

    Await.result(
      SharesTable.filterByTypeCd(typeCd),
      Duration(1, TimeUnit.MINUTES)
    ).map(s => ShareWrapper(s))
  }
}
