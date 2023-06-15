package ru.mikhaildruzhinin.trader.database

import java.time.Instant

object Models {
  case class Share(id: Long,
                   typeCd: Int,
                   figi: String,
                   lot: Int,
                   currency: String,
                   name: String,
                   exchange: String,
                   startingPrice: Option[BigDecimal],
                   purchasePrice: Option[BigDecimal],
                   currentPrice: Option[BigDecimal],
                   updateDttm: Option[Instant],
                   testFlg: Boolean,
                   loadDttm: Instant)

  type ShareModel = Share

  type ShareType = (
    Int,
    String,
    Int,
    String,
    String,
    String,
    Option[BigDecimal],
    Option[BigDecimal],
    Option[BigDecimal],
    Option[Instant],
    Boolean
  )
}
