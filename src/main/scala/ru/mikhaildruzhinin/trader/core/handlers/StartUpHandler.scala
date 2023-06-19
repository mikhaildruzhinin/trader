package ru.mikhaildruzhinin.trader.core.handlers

import com.github.kagkarlsson.scheduler.task.{ExecutionContext, TaskInstance, VoidExecutionHandler}
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.mikhaildruzhinin.trader.database.connection.Connection
import ru.mikhaildruzhinin.trader.database.tables.{SharesLogTable, SharesTable}

import scala.concurrent.Await

class StartUpHandler[T](implicit appConfig: AppConfig,
                        connection: Connection) extends VoidExecutionHandler[T] {

  override def execute(taskInstance: TaskInstance[T],
                       executionContext: ExecutionContext): Unit = {
    Await.result(
      connection.asyncRun(
        Vector(
          SharesTable.createIfNotExists,
          SharesLogTable.createIfNotExists
        )
      ),
      appConfig.slick.await.duration
    )
  }
}

object StartUpHandler {
  def apply()(implicit appConfig: AppConfig,
              connection: Connection) = new StartUpHandler[Void]()
}
