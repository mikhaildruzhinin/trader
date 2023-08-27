package ru.mikhaildruzhinin.trader.core.services.base

import ru.mikhaildruzhinin.trader.core.dto.AccountDTO

import scala.concurrent.Future

trait BaseAccountService {
  def getAccount: Future[AccountDTO]
}
