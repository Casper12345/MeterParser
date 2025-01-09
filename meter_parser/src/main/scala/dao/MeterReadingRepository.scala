package dao

import doobie.*
import doobie.postgres.implicits.*
import doobie.implicits.*
import cats.effect.IO
import domain.MeterReading
import doobie.hikari.HikariTransactor
import java.time.LocalDate

object MeterReadingRepository {

  def insertMeterReadings(readings: List[MeterReading])(using xa: HikariTransactor[IO]): IO[Int] = {
    val sql =
      """
        INSERT INTO meter_readings (nmi, timestamp, consumption)
        VALUES (?, ?, ?) ON CONFLICT (nmi, timestamp) DO UPDATE SET consumption = EXCLUDED.consumption
      """
    Update[MeterReading](sql).updateMany(readings).transact(xa)
  }
  
}
