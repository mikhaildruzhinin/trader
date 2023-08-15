package ru.mikhaildruzhinin.trader.core.services.base

import ru.mikhaildruzhinin.trader.core.wrappers.AccountWrapper

import scala.concurrent.Future

trait BaseAccountService {
  def getAccount: Future[AccountWrapper]
}
