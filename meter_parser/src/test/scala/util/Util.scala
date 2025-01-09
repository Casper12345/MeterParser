package util

import dao.Transactor
import java.nio.file.Path
import scala.io.Source
import cats.effect.unsafe.implicits.global
import doobie.postgres.implicits.*
import doobie.implicits.*
import cats.effect.IO
import domain.MeterReading
import doobie.hikari.HikariTransactor

object FileUtil {

  def getPath(filePath: String): Path =
    Path.of(getClass.getClassLoader.getResource(filePath).toURI)

  def getLines(filePath: String): Array[String] =
    Source.fromInputStream(getClass.getClassLoader.getResourceAsStream(filePath)).getLines().toArray
}

object DbUtil {

  def truncateTable(using xa: HikariTransactor[IO]): IO[Unit] = {
    val sql = sql"TRUNCATE TABLE  meter_readings"
    sql.update.run.transact(xa).void
  }

  def getAllMeterReadings(using xa: HikariTransactor[IO]): IO[List[MeterReading]] = {
    val sql = sql"SELECT nmi, timestamp, consumption FROM meter_readings ORDER BY nmi, timestamp"
    sql.query[MeterReading].to[List].transact(xa)
  }

  def truncateTable(): Unit = {
    Transactor.transactor.use { xa =>
      given HikariTransactor[IO] = xa

      DbUtil.truncateTable
    }.unsafeRunSync()
  }
}
