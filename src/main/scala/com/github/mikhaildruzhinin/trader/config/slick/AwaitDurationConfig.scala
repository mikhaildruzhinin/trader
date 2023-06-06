package com.github.mikhaildruzhinin.trader.config.slick

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.{Duration, FiniteDuration}

case class AwaitDurationConfig(length: Int,
                               timeUnit: TimeUnit) {

  val duration: FiniteDuration = Duration(length, timeUnit)
}
