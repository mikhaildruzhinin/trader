package ru.mikhaildruzhinin.trader.core.services

import ru.mikhaildruzhinin.trader.core.services.base._

case class Services (shareService: BaseShareService,
                     historicCandleService: BaseHistoricCandleService,
                     priceService: BasePriceService,
                     accountService: BaseAccountService)
