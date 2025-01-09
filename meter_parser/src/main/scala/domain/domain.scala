package domain 

import java.time.LocalDate

enum NEM12Record(indicator: Int):
  case Header(indicator: Int = 100) extends NEM12Record(indicator)
  case NMIDataDetails(indicator: Int = 200, nmi: String, intervalLength: Int) extends NEM12Record(indicator)
  case IntervalData(indicator: Int = 300, intervalDate: LocalDate, intervalValues: List[Double]) extends NEM12Record(indicator)
  case Footer(indicator: Int = 900) extends NEM12Record(indicator)
  case ParsableRecord(indicator: Int) extends NEM12Record(indicator)

case class MeterReading(nmi: String, timestamp: LocalDate, consumption: Double)
