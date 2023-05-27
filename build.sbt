ThisBuild / scalaVersion     := "2.13.10"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.github.mikhaildruzhinin"
ThisBuild / organizationName := "mikhaildruzhinin"

resolvers += "Secured Central Repository" at "https://repo.maven.apache.org/maven2"

externalResolvers := Resolver.combineDefaultResolvers(resolvers.value.toVector, mavenCentral = false)

lazy val root = (project in file("."))
  .settings(
    name := "trader"
  )

enablePlugins(JettyPlugin)

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.4.6",
  "com.github.kagkarlsson" % "db-scheduler" % "12.1.0",
  "com.github.pureconfig" %% "pureconfig" % "0.17.2",
  "com.typesafe" % "config" % "1.4.2",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
  "com.typesafe.slick" %% "slick" % "3.4.1",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.4.1",
  "javax.servlet" % "javax.servlet-api" % "4.0.1" % "provided",
  "org.postgresql" % "postgresql" % "42.6.0",
  "ru.tinkoff.piapi" % "java-sdk-core" % "1.3"
)
