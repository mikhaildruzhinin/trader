ThisBuild / scalaVersion     := "2.13.10"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "ru.mikhaildruzhinin"
ThisBuild / organizationName := "mikhaildruzhinin"

resolvers += "Secured Central Repository" at "https://repo.maven.apache.org/maven2"

externalResolvers := Resolver.combineDefaultResolvers(resolvers.value.toVector, mavenCentral = false)

lazy val root = (project in file("."))
  .configs(IntegrationTest)
  .settings(
    name := "trader",
    Defaults.itSettings
  )

enablePlugins(JettyPlugin)

val resilience4jVersion = "1.7.0"
val testcontainersVersion = "1.17.6"

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.4.6",
  "com.github.kagkarlsson" % "db-scheduler" % "12.2.0",
  "com.github.pureconfig" %% "pureconfig" % "0.17.2",
  "com.typesafe" % "config" % "1.4.2",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
  "com.typesafe.slick" %% "slick" % "3.4.1",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.4.1",
  "io.github.resilience4j" % "resilience4j-ratelimiter" % resilience4jVersion,
  "io.github.resilience4j" % "resilience4j-retry" % resilience4jVersion,
  "javax.servlet" % "javax.servlet-api" % "4.0.1" % "provided",
  "org.postgresql" % "postgresql" % "42.6.0",
  "ru.tinkoff.piapi" % "java-sdk-core" % "1.5",
  "org.scalatest" %% "scalatest" % "3.2.16" % "it,test",
  "org.testcontainers" % "postgresql" % testcontainersVersion % "it",
  "org.testcontainers" % "testcontainers" % testcontainersVersion % "it"
)

assembly / artifact := {
  val art = (assembly / artifact).value
  art.withClassifier(Some("assembly"))
}

addArtifact(assembly / artifact, assembly)

assembly / assemblyMergeStrategy := {
  case path if path.contains("META-INF/services") => MergeStrategy.concat
  case PathList("META-INF", _*) => MergeStrategy.discard
  case _ => MergeStrategy.first
}

assembly / assemblyShadeRules := Seq(
  ShadeRule
    .rename("shapeless.**" -> "new_shapeless.@1")
    .inAll
)

IntegrationTest / fork := true
