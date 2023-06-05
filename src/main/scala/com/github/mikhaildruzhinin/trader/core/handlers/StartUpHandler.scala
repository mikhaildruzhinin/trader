package com.github.mikhaildruzhinin.trader.core.handlers

import com.github.kagkarlsson.scheduler.task.{ExecutionContext, TaskInstance, VoidExecutionHandler}
import com.github.mikhaildruzhinin.trader.config.AppConfig
import com.github.mikhaildruzhinin.trader.database.SharesTable

import scala.concurrent.Await

class StartUpHandler[T](implicit appConfig: AppConfig) extends VoidExecutionHandler[T] {
  override def execute(taskInstance: TaskInstance[T],
                       executionContext: ExecutionContext): Unit = Await.ready(
    awaitable = SharesTable.createIfNotExists,
    atMost = appConfig.slick.await.duration
  )
}

object StartUpHandler {
  def apply()(implicit appConfig: AppConfig) = new StartUpHandler[Void]()
}
