package ru.mikhaildruzhinin.trader.config

import pureconfig._
import pureconfig.generic.ProductHint
import pureconfig.generic.auto.exportReader
import pureconfig.generic.semiauto.deriveEnumerationReader

object ConfigReader {
  implicit def hint[A]: ProductHint[A] = ProductHint[A](ConfigFieldMapping(CamelCase, CamelCase))

  implicit lazy val investApiModeConvert: ConfigReader[InvestApiMode] = deriveEnumerationReader[InvestApiMode]

  lazy val appConfig: AppConfig = ConfigSource.default.loadOrThrow[AppConfig]
}
