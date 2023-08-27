import com.typesafe.config.ConfigFactory

val conf = ConfigFactory
  .parseFile(new File("src/main/resources/application.conf"))
  .resolve()

ThisBuild / scalaVersion     := "2.13.10"
ThisBuild / version          := conf.getString("app.version")
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

val testcontainersVersion = "1.17.6"

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.4.6",
  "com.github.kagkarlsson" % "db-scheduler" % "12.4.0",
  "com.github.pureconfig" %% "pureconfig" % "0.17.2",
  "com.softwaremill.retry" %% "retry" % "0.3.6",
  "com.typesafe" % "config" % "1.4.2",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
  "com.typesafe.slick" %% "slick" % "3.4.1",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.4.1",
  "io.github.resilience4j" % "resilience4j-ratelimiter" % "1.7.0",
  "org.postgresql" % "postgresql" % "42.6.0",
  "ru.tinkoff.piapi" % "java-sdk-core" % "1.5",
  "org.scalatest" %% "scalatest" % "3.2.16" % "it,test",
  "org.flywaydb" % "flyway-core" % "9.16.0" % "it",
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

enablePlugins(FlywayPlugin)

val host = conf.getString("slick.db.properties.serverName")
val port = conf.getString("slick.db.properties.portNumber")
val db = conf.getString("slick.db.properties.databaseName")
val dbUrl = s"jdbc:postgresql://$host:$port/$db"
val user = conf.getString("slick.db.properties.user")
val password = conf.getString("slick.db.properties.password")

flywayUrl := dbUrl
flywayUser := user
flywayPassword := password
flywaySchemas := Seq("trader")

enablePlugins(CodegenPlugin)

slickCodegenDatabaseUrl := dbUrl
slickCodegenDatabaseUser := user
slickCodegenDatabasePassword := password
slickCodegenDriver :=  slick.jdbc.PostgresProfile
slickCodegenJdbcDriver := "org.postgresql.Driver"
slickCodegenOutputPackage := "ru.mikhaildruzhinin.trader.database.tables.codegen"
slickCodegenOutputDir := (sourceDirectory).value / "main" / "scala"
slickCodegenOutputToMultipleFiles := true
