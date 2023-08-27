package ru.mikhaildruzhinin.trader.core

import com.typesafe.scalalogging.Logger
import ru.mikhaildruzhinin.trader.core.services.Services

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Sell {
  val log: Logger = Logger(getClass.getName)

  def apply(services: Services): Future[Int] = for {
    shares <- services.shareService.getPersistedShares(TypeCode.Purchased)
    soldSharesNum <- services.shareService.persistUpdatedShares(shares, TypeCode.Sold)
    _ <- Future(log.info(s"Sold: ${soldSharesNum.sum}"))
    _ <- Future(shares.foreach(s => log.info(s.toString)))
  } yield soldSharesNum.sum
}
