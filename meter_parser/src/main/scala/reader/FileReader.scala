package reader

import app.Conf
import cats.effect.IO
import fs2.io.file.{Files, Path as FPath}
import fs2.{Stream, text}
import parser.NEM12Parser
import dao.MeterReadingRepository
import doobie.hikari.HikariTransactor
import java.nio.file.{Paths, Files as JFiles}
import scala.collection.mutable
import scala.jdk.CollectionConverters.*
import scala.language.postfixOps
import domain.{MeterReading, NEM12Record}

object FileReader extends Conf {

  private val chunkSize = config.getInt("reader.chunk_size")
  private val IntervalDividend = 1440

  def listFiles(directory: String): IO[List[FPath]] = IO(
    JFiles.list(Paths.get(directory)).iterator().asScala.toList.map(FPath.fromNioPath)
  )

  private[reader] def readFile(filePath: FPath): Stream[IO, String] =
    Files[IO]
      .readAll(filePath)
      .through(text.utf8.decode)
      .through(text.lines)
      .filterNot(_.isBlank)

  extension (b: Boolean)
    private def orRaise[A](f: => IO[A])(e: Exception): IO[A] =
      if (b) f else IO.raiseError(e)
  
  /**
   * Each individual processFile stream is processed sequentially, so we don't need to worry about race conditions
   * when mutating variables. Hence, no need for expensive locking.
   * Results are written to the database in chunks and the full set is garbage collected after each chunk.
   * The result set is updated with the latest result to support ON CONFLICT DO UPDATE semantics.
   */
  def processFile(filePath: FPath)(using xa: HikariTransactor[IO]): Stream[IO, Unit] = {
    var intervalLength = 0
    var maybeNmi: Option[String] = None
    var resultSet = mutable.Set.empty[MeterReading]
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
            val reading = MeterReading(nmi, intervalDate, intervalValues.sum)
            if (!resultSet.contains(reading)) {
              resultCount = resultCount + 1
            }
            resultSet.addOne(reading) // override as we update.

            if (resultCount >= chunkSize) {
              val xs = resultSet.toList
              resultSet = mutable.Set.empty[MeterReading] // best for GC
              resultCount = 0
              MeterReadingRepository.insertMeterReadings(xs).void
            } else IO.unit
          }(GeneralFileReaderException("Intervals do not match interval length"))
        }

      case Right(NEM12Record.Footer(_)) =>
        (isHeaderSet && maybeNmi.isDefined).orRaise {
          (!isFooterSet).orRaise {
            isFooterSet = true
            MeterReadingRepository.insertMeterReadings(resultSet.toList).void
          }(InvalidOrderException("Cannot parse footer twice"))
        }(InvalidOrderException("Footer has to be parsed last"))

      case Left(e) => IO.raiseError(e)
      case _ => IO.unit
    }
  }
}
