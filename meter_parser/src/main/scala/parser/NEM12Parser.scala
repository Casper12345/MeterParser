package parser

import java.time.format.DateTimeFormatter
import java.time.LocalDate
import scala.util.Try
import cats.syntax.all.*
import domain.{NEM12Record, ParserRecord}

trait MeterFileParser {
  /**
   * Trait describes a file parser that reads lines as strings and returns either an exception or a parsed record.
   * The only constraint on the Parser record is that it must hold a record indicator.  
   *
   * @param line string to parse
   */
  def parseLine(line: String): Either[ParserException, ParserRecord]

}

/**
 * Implementation of the MeterFileParser that parses NEM12 records.
 * See https://aemo.com.au/-/media/files/electricity/nem/retail_and_metering/metering-procedures/2017/mdff_specification_nem12_nem13_final_v102.pdf
 * for the specification.
 */
object NEM12Parser extends MeterFileParser {

  private val DateFormat = "yyyyMMdd"

  private def parseDate(dateString: String): Either[ParserException, LocalDate] =
    Try(LocalDate.parse(dateString, DateTimeFormatter.ofPattern(DateFormat)))
      .toEither.leftMap(e => InvalidDataException(s"Failed to parse date ${e.getMessage}"))

  private def parseInt(intString: String): Either[ParserException, Int] =
    Try(intString.toInt).toEither.leftMap(e => InvalidDataException(s"Failed to parse integer ${e.getMessage}"))

  private def parseDouble(doubleString: String): Either[ParserException, Double] =
    Try(doubleString.toDouble).toEither.leftMap(e => InvalidDataException(s"Failed to parse double ${e.getMessage}"))

  def parseLine(line: String): Either[ParserException, NEM12Record] = {
    val fields = line.split(",").map(_.strip)
    fields.headOption.toRight(GeneralParserException("Empty line")).flatMap(parseInt).flatMap {
      case 100 if fields.length == 5 =>
        if fields(1).strip == "NEM12" then Right(NEM12Record.Header())
        else Left(InvalidFormatException("Parser only supports NEM12 format"))
      case 200 if fields.length == 10 =>
        if fields.length == 10 then parseInt(fields(8).strip)
          .filterOrElse(i => i == 5 || i == 15 || i == 30, InvalidFormatException(s"Invalid NEM12 interval length"))
          .map(length => NEM12Record.NMIDataDetails(nmi = fields(1).strip, intervalLength = length))
        else Left(InvalidFormatException("Invalid NEM12 NMIDataDetails record"))
      case 300 if fields.length >= 9 =>
        for {
          intervalValues <- fields.slice(2, fields.length - 5).map(parseDouble).toList.sequence
          date <- parseDate(fields(1))
        } yield NEM12Record.IntervalData(intervalDate = date, intervalValues = intervalValues)
      case 900 if fields.length == 1 => Right(NEM12Record.Footer())
      case x if x == 500 || x == 400 => Right(NEM12Record.ParsableRecord(x))
      case x => Left(InvalidFormatException(s"Invalid NEM12 record indicator $x"))
    }
  }

}
