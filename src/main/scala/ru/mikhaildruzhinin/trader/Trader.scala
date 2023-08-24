package ru.mikhaildruzhinin.trader

import java.time.Instant

object Trader extends App with Components {

  scheduler.start()

  scheduler
    .schedule(
      startUpTask.instance("1"),
      Instant.now.plusSeconds(5)
    )

}
