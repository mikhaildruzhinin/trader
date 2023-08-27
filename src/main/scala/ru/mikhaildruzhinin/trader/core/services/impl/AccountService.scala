package ru.mikhaildruzhinin.trader.core.services.impl

import ru.mikhaildruzhinin.trader.client.base.BaseInvestApiClient
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.mikhaildruzhinin.trader.core.services.base.BaseAccountService
import ru.mikhaildruzhinin.trader.core.dto.AccountDTO

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class AccountService(investApiClient: BaseInvestApiClient)
                    (implicit appConfig: AppConfig) extends BaseAccountService {

  override def getAccount: Future[AccountDTO] = for {
    account <- investApiClient.getAccount
    wrappedAccount <- Future { AccountDTO(account) }
  } yield wrappedAccount
}
