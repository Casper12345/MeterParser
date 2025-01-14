package reader

import cats.effect.IO
import fs2.io.file.Path as FPath
import munit.CatsEffectSuite
import dao.Transactor
import doobie.hikari.HikariTransactor
import domain.BaseMeterReading
import util.{DbUtil, FileUtil}

import java.nio.file.Files as JFiles
import java.time.{LocalDate, LocalDateTime}
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
    Transactor.transactor.use { xa =>
      given HikariTransactor[IO] = xa

      for {
        _ <- Nem12FileReader().processFile(filePath).compile.drain
        r1 <- DbUtil.getMeterReadings("NEM1201015")
        _ <- DbUtil.cleanTable("NEM1201015")
      } yield {
        assertEquals(r1.length, 192)
        assertEquals(r1.head, BaseMeterReading("NEM1201015", LocalDateTime.parse("2005-03-01T00:00"), 0.0))
        assertEquals(r1(1), BaseMeterReading("NEM1201015", LocalDateTime.parse("2005-03-01T00:30"), 0.0))
        assertEquals(r1(47), BaseMeterReading("NEM1201015", LocalDateTime.parse("2005-03-01T23:30"), 0.231))
        assertEquals(r1(48), BaseMeterReading("NEM1201015", LocalDateTime.parse("2005-03-02T00:00"), 0.0))
        assertEquals(r1(95), BaseMeterReading("NEM1201015", LocalDateTime.parse("2005-03-02T23:30"), 0.432))
        assertEquals(r1(96), BaseMeterReading("NEM1201015", LocalDateTime.parse("2005-03-03T00:00"), 0.0))
        assertEquals(r1(143), BaseMeterReading("NEM1201015", LocalDateTime.parse("2005-03-03T23:30"), 0.321))
        assertEquals(r1(144), BaseMeterReading("NEM1201015", LocalDateTime.parse("2005-03-04T00:00"), 0.0))
        assertEquals(r1(191), BaseMeterReading("NEM1201015", LocalDateTime.parse("2005-03-04T23:30"), 0.612))
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
