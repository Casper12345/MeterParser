package reader

import cats.effect.IO
import fs2.io.file.Path as FPath
import munit.CatsEffectSuite
import dao.Transactor
import doobie.hikari.HikariTransactor
import domain.MeterReading
import util.{DbUtil, FileUtil}
import java.nio.file.Files as JFiles
import java.time.LocalDate
import scala.jdk.CollectionConverters.*
import scala.language.postfixOps

class FileReaderSuite extends CatsEffectSuite {

  private val tempDir = JFiles.createTempDirectory("testDir")

  override def afterAll(): Unit = {
    tempDir.toFile.delete()
    DbUtil.truncateTable()
  }

  override def beforeEach(context: BeforeEach): Unit = {
    DbUtil.truncateTable()
  }

  private def createFile(fileName: String, lines: List[String]): FPath = {
    val path = tempDir.resolve(fileName)
    JFiles.write(path, lines.asJava)
    FPath.fromNioPath(path)
  }

  test("listFiles should return all files in a directory") {
    val file1 = createFile("file1.txt", List("line1", "line2"))
    val file2 = createFile("file2.txt", List("lineA", "lineB"))

    FileReader.listFiles(tempDir.toString).map(_.map(_.fileName.toString).sorted)
      .assertEquals(List("file1.txt", "file2.txt"))
  }

  test("readFile should read non-blank lines from a file") {
    val filePath = createFile("file3.txt", List("", "line1", "line2", "", "line3"))

    FileReader.readFile(filePath).compile.toList
      .assertEquals(List("line1", "line2", "line3"))
  }

  test("readFile should parse valid file") {
    val filePath = FPath.fromNioPath(FileUtil.getPath("csv/valid.csv"))
    val expected =
      List(
        MeterReading(
          nmi = "NEM1201015",
          timestamp = LocalDate.parse("2005-03-03"),
          consumption = 29.789
        ),
        MeterReading(
          nmi = "NEM1201016",
          timestamp = LocalDate.parse("2005-03-01"),
          consumption = 33.19
        ),
        MeterReading(
          nmi = "NEM1201015",
          timestamp = LocalDate.parse("2005-03-02"),
          consumption = 32.24
        ),
        MeterReading(
          nmi = "NEM1201016",
          timestamp = LocalDate.parse("2005-03-02"),
          consumption = 31.811
        ),
        MeterReading(
          nmi = "NEM1201016",
          timestamp = LocalDate.parse("2005-03-04"),
          consumption = 31.354
        ),
        MeterReading(
          nmi = "NEM1201016",
          timestamp = LocalDate.parse("2005-03-03"),
          consumption = 34.204
        ),
        MeterReading(
          nmi = "NEM1201015",
          timestamp = LocalDate.parse("2005-03-04"),
          consumption = 34.206
        ),
        MeterReading(
          nmi = "NEM1201015",
          timestamp = LocalDate.parse("2005-03-01"),
          consumption = 31.444
        )
      ).sortBy(m => (m.nmi, m.timestamp))

    Transactor.transactor.use { xa =>
      given HikariTransactor[IO] = xa

      for {
        r1 <- DbUtil.getAllMeterReadings
        _ <- FileReader.processFile(filePath).compile.drain
        r2 <- DbUtil.getAllMeterReadings
      } yield {
        assert(r1.isEmpty)
        assertEquals(r2, expected)
      }
    }
  }

  test("file with header not first should report error") {
    given HikariTransactor[IO] = null

    val filePath = FPath.fromNioPath(FileUtil.getPath("csv/header_not_first.csv"))

    FileReader.processFile(filePath).compile.drain.handleError {
      case e: InvalidOrderException =>
        assertEquals(e.getMessage, "Header has to be parsed before NMIDataDetails")
    }
  }

  test("file with header not first should report error") {
    given HikariTransactor[IO] = null

    val filePath = FPath.fromNioPath(FileUtil.getPath("csv/header_not_first.csv"))

    FileReader.processFile(filePath).compile.drain.handleError {
      case e: InvalidOrderException =>
        assertEquals(e.getMessage, "Header has to be parsed before NMIDataDetails")
    }
  }

  test("file with more than one header should report error") {
    given HikariTransactor[IO] = null

    val filePath = FPath.fromNioPath(FileUtil.getPath("csv/multiple_headers.csv"))

    FileReader.processFile(filePath).compile.drain.handleError {
      case e: InvalidOrderException =>
        assertEquals(e.getMessage, "Cannot parse header twice")
    }
  }

  test("file with 300 before 200 should report error") {
    given HikariTransactor[IO] = null

    val filePath = FPath.fromNioPath(FileUtil.getPath("csv/300_before_200.csv"))

    FileReader.processFile(filePath).compile.drain.handleError {
      case e: InvalidOrderException =>
        assertEquals(e.getMessage, "NMIDataDetails has to be parsed before IntervalData")
    }
  }

  test("file where number of intervals does not match interval length should report error") {
    given HikariTransactor[IO] = null

    val filePath = FPath.fromNioPath(FileUtil.getPath("csv/length_mismatch.csv"))

    FileReader.processFile(filePath).compile.drain.handleError {
      case e: GeneralFileReaderException =>
        assertEquals(e.getMessage, "Intervals do not match interval length")
    }
  }

  test("file where 900 comes 200 should report error") {

    val filePath = FPath.fromNioPath(FileUtil.getPath("csv/900_not_last.csv"))

    given HikariTransactor[IO] = null

    FileReader.processFile(filePath).compile.drain.handleError {
      case e: InvalidOrderException =>
        assertEquals(e.getMessage, "Footer has to be parsed last")
    }
  }

}
