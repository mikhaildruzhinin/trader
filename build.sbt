ThisBuild / scalaVersion     := "2.13.10"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "ru.mikhaildruzhinin"
ThisBuild / organizationName := "mikhaildruzhinin"

resolvers += "Secured Central Repository" at "https://repo.maven.apache.org/maven2"

externalResolvers := Resolver.combineDefaultResolvers(resolvers.value.toVector, mavenCentral = false)

lazy val root = (project in file("."))
  .settings(
    name := "trader"
  )

val scalatestVersion = "3.2.17"

libraryDependencies ++= Seq(
  "org.scalactic" %% "scalactic" % scalatestVersion,
  "org.scalatest" %% "scalatest" % scalatestVersion % "test",
  "ru.tinkoff.piapi" % "java-sdk-core" % "1.6",
  "com.github.pureconfig" %% "pureconfig" % "0.17.4",
  "org.ta4j" % "ta4j-core" % "0.15"
)

Test / fork := true
