package reader

import app.Conf
import cats.effect.IO
import fs2.io.file.{Files, Path as FPath}
import fs2.{Stream, text}
import parser.NEM12Parser
import dao.PostgresMeterReadingRepository
import doobie.hikari.HikariTransactor

import java.nio.file.{Paths, Files as JFiles}
import scala.collection.mutable
import scala.jdk.CollectionConverters.*
import scala.language.postfixOps
import domain.{BaseMeterReading, NEM12Record}
import FileReader.{*, given}
import com.typesafe.scalalogging.LazyLogging

import java.time.temporal.ChronoUnit

/**
 * This trait describes a fs2 based stream file processor. The effect type is always a cats.effect.IO.
 *
 * @tparam A return type of the stream. 
 */
trait FileReader[A] {
  def processFile(filePath: FPath): Stream[IO, A]
}

/**
 * This object contains utility methods for all implementations of FileReader.
 */
object FileReader {

  def listFiles(directory: String): IO[List[FPath]] = IO.blocking(
    JFiles.list(Paths.get(directory)).iterator().asScala.toList.map(FPath.fromNioPath)
  )

  private[reader] def readFile(filePath: FPath): Stream[IO, String] =
    Files[IO]
      .readAll(filePath)
      .through(text.utf8.decode)
      .through(text.lines)
      .filterNot(_.isBlank)

  extension (b: Boolean)
    private[reader] def orRaise[A](f: => IO[A])(e: Exception): IO[A] =
      if (b) f else IO.raiseError(e)

}

/**
 * Constructor object for the Nem12FileReader.
 * Creating only one instance of Nem12FileReader which is cached throughout the lifetime of the jvm.
 */
object Nem12FileReader {
  private var fileReader: Option[Nem12FileReader] = None

  def apply()(using xa: HikariTransactor[IO]): Nem12FileReader =
    synchronized {
      if (fileReader.isEmpty) {
        fileReader = Some(new Nem12FileReader(xa))
        fileReader.get
      } else fileReader.get
    }
}

/**
 * Nem12 implementation of FileReader.
 * This implementation preserves the order of the records in the file and shuts down the stream
 * whenever an error is encountered.
 *
 * @param xa HikariTransactor db transactions.
 */
private class Nem12FileReader(xa: HikariTransactor[IO]) extends FileReader[Unit] with Conf with LazyLogging {

  private val chunkSize = config.getInt("reader.chunk_size")
  private val IntervalDividend = 1440

  private given HikariTransactor[IO] = xa

  /**
   * Each individual processFile stream is processed sequentially, so we don't need to worry about any race conditions
   * when mutating variables. Hence, no need for expensive locking.
   * Results are written to the database in chunks and the full set is garbage collected after each chunk.
   * The result set is updated with the latest result to support ON CONFLICT DO UPDATE semantics.
   */
  def processFile(filePath: FPath): Stream[IO, Unit] = {
    var intervalLength = 0
    var maybeNmi: Option[String] = None
    var resultSet = mutable.Set.empty[BaseMeterReading]
    var resultCount = 0
    var isHeaderSet = false
    var isFooterSet = false

    readFile(filePath).map(NEM12Parser.parseLine).evalMap {
      case Right(NEM12Record.Header(_)) =>
        (!isHeaderSet).orRaise {
          isHeaderSet = true
          IO.unit
        }(InvalidOrderException("Cannot parse header twice"))

      case Right(NEM12Record.NMIDataDetails(_, nmi, length)) =>
        isHeaderSet.orRaise {
          intervalLength = length
          maybeNmi = Some(nmi)
          IO.unit
        }(InvalidOrderException("Header has to be parsed before NMIDataDetails"))

      case Right(NEM12Record.IntervalData(_, intervalDate, intervalValues)) =>
        maybeNmi.fold(IO.raiseError(
          InvalidOrderException("NMIDataDetails has to be parsed before IntervalData"))
        ) { nmi =>
          (intervalValues.size == (IntervalDividend / intervalLength)).orRaise {
            val lengthOfInterval = IntervalDividend / intervalLength
            val intervals = (0 until lengthOfInterval).map { i =>
              intervalDate.atStartOfDay().plus(i * intervalLength, ChronoUnit.MINUTES)
            }
            intervalValues.zip(intervals).map { (value, intervalDate) =>
              val reading = BaseMeterReading(nmi, intervalDate, value)
              if (!resultSet.contains(reading)) {
                resultCount = resultCount + 1
              }
              resultSet.addOne(reading) // override as we update.
            }

            if (resultCount >= chunkSize) {
              logger.debug(s"Inserting ${resultSet.size} records")
              val xs = resultSet.toList
              resultSet = mutable.Set.empty[BaseMeterReading] // best for GC
              resultCount = 0
              PostgresMeterReadingRepository().insertMeterReadings(xs).void
            } else IO.unit
          }(GeneralFileReaderException("Intervals do not match interval length"))
        }

      case Right(NEM12Record.Footer(_)) =>
        (isHeaderSet && maybeNmi.isDefined).orRaise {
          (!isFooterSet).orRaise {
            isFooterSet = true
            logger.debug(s"Inserting ${resultSet.size} records from footer")
            PostgresMeterReadingRepository().insertMeterReadings(resultSet.toList).void
          }(InvalidOrderException("Cannot parse footer twice"))
        }(InvalidOrderException("Footer has to be parsed last"))

      case Left(e) => IO.raiseError(e)
      case _ => IO.unit
    }
  }
}
