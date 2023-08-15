package ru.mikhaildruzhinin.trader.core.services.impl

import ru.mikhaildruzhinin.trader.client.base.BaseInvestApiClient
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.mikhaildruzhinin.trader.core.services.base.BaseAccountService
import ru.mikhaildruzhinin.trader.core.wrappers.AccountWrapper

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class AccountService(investApiClient: BaseInvestApiClient)
                    (implicit appConfig: AppConfig) extends BaseAccountService {

  override def getAccount: Future[AccountWrapper] = for {
    account <- investApiClient.getAccount
    wrappedAccount <- Future { AccountWrapper(account.getId) }
  } yield wrappedAccount
}
