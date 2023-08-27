addSbtPlugin("org.jetbrains.scala" % "sbt-ide-settings" % "1.1.1")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.1.1")
addSbtPlugin("io.github.davidmweber" % "flyway-sbt" % "7.4.0")
addSbtPlugin("com.github.tototoshi" % "sbt-slick-codegen" % "2.0.0")

libraryDependencies += "org.postgresql" % "postgresql" % "42.5.4"
