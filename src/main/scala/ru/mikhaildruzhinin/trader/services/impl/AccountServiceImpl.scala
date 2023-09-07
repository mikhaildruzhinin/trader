package ru.mikhaildruzhinin.trader.services.impl

import ru.mikhaildruzhinin.trader.client.InvestApiClient
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.mikhaildruzhinin.trader.models.AccountModel
import ru.mikhaildruzhinin.trader.services.AccountService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AccountServiceImpl(investApiClient: InvestApiClient)
                        (implicit appConfig: AppConfig) extends AccountService {

  override def getAccount: Future[AccountModel] = for {
    account <- investApiClient.getAccount
    wrappedAccount <- Future { AccountModel(account) }
  } yield wrappedAccount
}
