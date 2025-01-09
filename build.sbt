ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.6.2"

lazy val flyway = (project in file("flyway")).settings(
  name := "flyway",
  libraryDependencies ++= Seq(
    Dependencies.postgresql,
    Dependencies.flywayDatabasePostgresql,
    Dependencies.flywayCore,
    Dependencies.typesafeConfig
  ),
)

lazy val meterParser = (project in file("meter_parser")).settings(
  name := "meter_parser",
  libraryDependencies ++= Seq(
    Dependencies.fs2Core,
    Dependencies.fs2Io,
    Dependencies.catsEffect,
    Dependencies.doobieCore,
    Dependencies.doobieHikari,
    Dependencies.doobiePostgres,
    Dependencies.typesafeConfig,
    Dependencies.scalaLogging,
    Dependencies.logBack,
    TestDependencies.munit
  )
)

lazy val root = (project in file(".")).settings(
  assembly / test := {},
  ThisBuild / assemblyMergeStrategy := {
    case PathList("META-INF", xs@_*) =>
      xs.map {
        _.toLowerCase
      } match {
        case "services" :: xs =>
          MergeStrategy.filterDistinctLines
        case _ => MergeStrategy.discard
      }
    case "LICENSE-2.0.txt" => MergeStrategy.discard
    case "CONTRIBUTING" => MergeStrategy.discard
    case "LICENSE" => MergeStrategy.discard
    case "NOTICE" => MergeStrategy.discard
    case "rootdoc.txt" => MergeStrategy.discard
    case _ => MergeStrategy.first
  },
  assembly / assemblyOption := (assembly / assemblyOption).value.withIncludeScala(true),
  scalacOptions ++= Seq("-no-indent")
).aggregate(flyway, meterParser)
