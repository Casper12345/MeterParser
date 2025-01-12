package reader

import cats.effect.IO
import fs2.io.file.Path as FPath
import munit.CatsEffectSuite
import dao.Transactor
import doobie.hikari.HikariTransactor
import domain.BaseMeterReading
import util.{DbUtil, FileUtil}
import java.nio.file.Files as JFiles
import java.time.LocalDate
import scala.language.postfixOps

class FileReaderSuite extends CatsEffectSuite {

  private val tempDir = JFiles.createTempDirectory("testDir")

  override def afterAll(): Unit = {
    tempDir.toFile.delete()
  }

  test("listFiles should return all files in a directory") {
    val file1 = FileUtil.createFile("file1.txt", List("line1", "line2"), tempDir)
    val file2 = FileUtil.createFile("file2.txt", List("lineA", "lineB"), tempDir)

    FileReader.listFiles(tempDir.toString).map(_.map(_.fileName.toString).sorted)
      .assertEquals(List("file1.txt", "file2.txt"))
  }

  test("readFile should read non-blank lines from a file") {
    val filePath = FileUtil.createFile("file3.txt", List("", "line1", "line2", "", "line3"), tempDir)

    FileReader.readFile(filePath).compile.toList
      .assertEquals(List("line1", "line2", "line3"))
  }
}

class Nem12FileReaderSuite extends CatsEffectSuite {
  
  test("readFile should parse valid file") {
    val filePath = FPath.fromNioPath(FileUtil.getPath("csv/valid.csv"))
    val expected1 =
      List(
        BaseMeterReading(
          nmi = "NEM1201015",
          timestamp = LocalDate.parse("2005-03-03"),
          consumption = 29.789
        ),
        BaseMeterReading(
          nmi = "NEM1201015",
          timestamp = LocalDate.parse("2005-03-02"),
          consumption = 32.24
        ),
        BaseMeterReading(
          nmi = "NEM1201015",
          timestamp = LocalDate.parse("2005-03-04"),
          consumption = 34.206
        ),
        BaseMeterReading(
          nmi = "NEM1201015",
          timestamp = LocalDate.parse("2005-03-01"),
          consumption = 31.444
        )
      ).sortBy(m => (m.nmi, m.timestamp))

    val expected2 = List(
      BaseMeterReading(
        nmi = "NEM1201016",
        timestamp = LocalDate.parse("2005-03-02"),
        consumption = 31.811
      ),
      BaseMeterReading(
        nmi = "NEM1201016",
        timestamp = LocalDate.parse("2005-03-04"),
        consumption = 31.354
      ),
      BaseMeterReading(
        nmi = "NEM1201016",
        timestamp = LocalDate.parse("2005-03-03"),
        consumption = 34.204
      ),
      BaseMeterReading(
        nmi = "NEM1201016",
        timestamp = LocalDate.parse("2005-03-01"),
        consumption = 33.19
      ),
    ).sortBy(m => (m.nmi, m.timestamp))

    Transactor.transactor.use { xa =>
      given HikariTransactor[IO] = xa

      for {
        _ <- Nem12FileReader().processFile(filePath).compile.drain
        r1 <- DbUtil.getMeterReadings("NEM1201015")
        r2 <- DbUtil.getMeterReadings("NEM1201016")
        _<- DbUtil.cleanTable("NEM1201015")
        _<- DbUtil.cleanTable("NEM1201016")
      } yield {
        assertEquals(r1, expected1)
        assertEquals(r2, expected2)
      }
    }
  }

  test("file with header not first should report error") {
    given HikariTransactor[IO] = null

    val filePath = FPath.fromNioPath(FileUtil.getPath("csv/header_not_first.csv"))

    Nem12FileReader().processFile(filePath).compile.drain.handleError {
      case e: InvalidOrderException =>
        assertEquals(e.getMessage, "Header has to be parsed before NMIDataDetails")
    }
  }

  test("file with header not first should report error") {
    given HikariTransactor[IO] = null

    val filePath = FPath.fromNioPath(FileUtil.getPath("csv/header_not_first.csv"))

    Nem12FileReader().processFile(filePath).compile.drain.handleError {
      case e: InvalidOrderException =>
        assertEquals(e.getMessage, "Header has to be parsed before NMIDataDetails")
    }
  }

  test("file with more than one header should report error") {
    given HikariTransactor[IO] = null

    val filePath = FPath.fromNioPath(FileUtil.getPath("csv/multiple_headers.csv"))

    Nem12FileReader().processFile(filePath).compile.drain.handleError {
      case e: InvalidOrderException =>
        assertEquals(e.getMessage, "Cannot parse header twice")
    }
  }

  test("file with 300 before 200 should report error") {
    given HikariTransactor[IO] = null

    val filePath = FPath.fromNioPath(FileUtil.getPath("csv/300_before_200.csv"))

    Nem12FileReader().processFile(filePath).compile.drain.handleError {
      case e: InvalidOrderException =>
        assertEquals(e.getMessage, "NMIDataDetails has to be parsed before IntervalData")
    }
  }

  test("file where number of intervals does not match interval length should report error") {
    given HikariTransactor[IO] = null

    val filePath = FPath.fromNioPath(FileUtil.getPath("csv/length_mismatch.csv"))

    Nem12FileReader().processFile(filePath).compile.drain.handleError {
      case e: GeneralFileReaderException =>
        assertEquals(e.getMessage, "Intervals do not match interval length")
    }
  }

  test("file where 900 comes 200 should report error") {

    val filePath = FPath.fromNioPath(FileUtil.getPath("csv/900_not_last.csv"))

    given HikariTransactor[IO] = null

    Nem12FileReader().processFile(filePath).compile.drain.handleError {
      case e: InvalidOrderException =>
        assertEquals(e.getMessage, "Footer has to be parsed last")
    }
  }

}
