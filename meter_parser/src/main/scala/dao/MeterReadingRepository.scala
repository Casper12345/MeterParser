package dao

import doobie.*
import doobie.postgres.implicits.*
import doobie.implicits.*
import cats.effect.IO
import domain.{BaseMeterReading, MeterReading}
import doobie.hikari.HikariTransactor
import java.time.LocalDate

/**
 * Abstract MeterReading dao repository that can be implemented for different databases.
 * The effect type is always a cats.effect.IO. 
 *
 * @tparam A the type of meter reading to insert
 * @tparam B the type of return value          
 */
trait MeterReadingRepository[A <: MeterReading, B] {

  def insertMeterReadings(readings: List[A]): IO[B]

}

private[dao] class PostgresMeterReadingRepository(xa: HikariTransactor[IO]) extends MeterReadingRepository[BaseMeterReading, Int] {

  def insertMeterReadings(readings: List[BaseMeterReading]): IO[Int] = {
    val sql =
      """
          INSERT INTO meter_readings (nmi, timestamp, consumption)
          VALUES (?, ?, ?) ON CONFLICT (nmi, timestamp) DO UPDATE SET consumption = EXCLUDED.consumption
        """
    Update[BaseMeterReading](sql).updateMany(readings).transact(xa)
  }
}

object PostgresMeterReadingRepository {
  private var repository: Option[PostgresMeterReadingRepository] = None

  def apply()(using xa: HikariTransactor[IO]): PostgresMeterReadingRepository =
    if (repository.isEmpty) {
      repository = Some(new PostgresMeterReadingRepository(xa))
      repository.get
    } else repository.get
}