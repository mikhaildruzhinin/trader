package ru.mikhaildruzhinin.trader.core.handlers

import com.typesafe.scalalogging.Logger
import ru.mikhaildruzhinin.trader.client.BaseInvestApiClient
import ru.mikhaildruzhinin.trader.config.AppConfig
import ru.mikhaildruzhinin.trader.core.{ShareWrapper, TypeCode}
import ru.mikhaildruzhinin.trader.database.connection.Connection
import ru.mikhaildruzhinin.trader.database.tables.SharesTable

trait Handler {
  val log: Logger = Logger(getClass.getName)

  protected def wrapPersistedShares(typeCode: TypeCode)
                                   (implicit appConfig: AppConfig,
                                    connection: Connection): Seq[ShareWrapper] = {

    val code: Int = TypeCode.unapply(typeCode)
    val wrappedShares: Seq[ShareWrapper] = connection
      .run(SharesTable.filterByTypeCode(code))
      .flatten
      .map(s => ShareWrapper(s))

    log.info(s"Got ${wrappedShares.length} shares of type: $typeCode($code)")
    wrappedShares
  }

  def apply()(implicit appConfig: AppConfig,
              investApiClient: BaseInvestApiClient,
              connection: Connection): Int
}
