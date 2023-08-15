package ru.mikhaildruzhinin.trader.core.wrappers

import ru.tinkoff.piapi.contract.v1.Account

case class AccountWrapper(id: String) {

  def this(account: Account) = this(account.getId)
}
