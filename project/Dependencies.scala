import sbt.*

object Dependencies {

  val flywayDatabasePostgresql = "org.flywaydb" % "flyway-database-postgresql" % "10.14.0"
  val flywayCore = "org.flywaydb" % "flyway-core" % "11.1.0"
  val typesafeConfig = "com.typesafe" % "config" % "1.4.3"
  val fs2Core = "co.fs2" %% "fs2-core" % "3.10.2"
  val fs2Io = "co.fs2" %% "fs2-io" % "3.10.2"
  val catsEffect = "org.typelevel" %% "cats-effect" % "3.5.4"
  val doobieCore = "org.tpolecat" %% "doobie-core" % "1.0.0-RC5"
  val doobieHikari = "org.tpolecat" %% "doobie-hikari" % "1.0.0-RC5"
  val doobiePostgres = "org.tpolecat" %% "doobie-postgres" % "1.0.0-RC5"
  val postgresql = "org.postgresql" % "postgresql" % "42.7.3"
  val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5"
  val logBack = "org.slf4j" % "slf4j-log4j12" % "2.0.13"
}

object TestDependencies {
  val munit = "org.typelevel" %% "munit-cats-effect" % "2.0.0" % Test
}