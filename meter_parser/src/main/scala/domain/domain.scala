package domain

import java.time.{LocalDate, LocalDateTime}

trait ParserRecord {
  def indicator: Int
}

enum NEM12Record(indicator: Int) extends ParserRecord:
  case Header(indicator: Int = 100) extends NEM12Record(indicator)
  case NMIDataDetails(indicator: Int = 200, nmi: String, intervalLength: Int) extends NEM12Record(indicator)
  case IntervalData(indicator: Int = 300, intervalDate: LocalDate, intervalValues: List[Double]) extends NEM12Record(indicator)
  case Footer(indicator: Int = 900) extends NEM12Record(indicator)
  case ParsableRecord(indicator: Int) extends NEM12Record(indicator)


abstract class MeterReading {
  val nmi: String
  val timestamp: LocalDateTime
  val consumption: Double
}

case class BaseMeterReading(nmi: String, timestamp: LocalDateTime, consumption: Double) extends MeterReading 
