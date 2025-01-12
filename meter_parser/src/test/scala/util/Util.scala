package util

import cats.effect.unsafe
import dao.Transactor
import java.nio.file.Path
import scala.io.Source
import doobie.postgres.implicits.*
import doobie.implicits.*
import cats.effect.IO
import domain.BaseMeterReading
import doobie.hikari.HikariTransactor
import fs2.io.file.Path as FPath
import java.nio.file.Files as JFiles
import scala.jdk.CollectionConverters.*

object FileUtil {

  def getPath(filePath: String): Path =
    Path.of(getClass.getClassLoader.getResource(filePath).toURI)

  def getLines(filePath: String): Array[String] =
    Source.fromInputStream(getClass.getClassLoader.getResourceAsStream(filePath)).getLines().toArray

  def createFile(fileName: String, lines: List[String], tempDir: Path): FPath = {
    val path = tempDir.resolve(fileName)
    JFiles.write(path, lines.asJava)
    FPath.fromNioPath(path)
  }  
    
}

object DbUtil {

  def cleanTable(nmi: String)(using xa: HikariTransactor[IO]): IO[Unit] = {
    val sql = sql"DELETE FROM meter_readings WHERE nmi = $nmi"
    sql.update.run.transact(xa).void
  }

  def getMeterReadings(nmi: String)(using xa: HikariTransactor[IO]): IO[List[BaseMeterReading]] = {
    val sql = sql"SELECT nmi, timestamp, consumption FROM meter_readings WHERE nmi = $nmi ORDER BY nmi, timestamp"
    sql.query[BaseMeterReading].to[List].transact(xa)
  }
  
}
