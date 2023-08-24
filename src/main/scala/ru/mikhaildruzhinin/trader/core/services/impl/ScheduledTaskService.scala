package ru.mikhaildruzhinin.trader.core.services.impl

import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.mikhaildruzhinin.trader.core.services.base.BaseScheduledTaskService
import ru.mikhaildruzhinin.trader.database.Connection
import ru.mikhaildruzhinin.trader.database.tables.ScheduledTaskDAO

class ScheduledTaskService(connection: Connection,
                           scheduledTaskDAO: ScheduledTaskDAO)
                          (implicit appConfig: AppConfig) extends BaseScheduledTaskService {

  override def createScheduledTasks(): Unit = {
    scheduledTaskDAO.createIfNotExists
    scheduledTaskDAO.createIndices
  }
}
