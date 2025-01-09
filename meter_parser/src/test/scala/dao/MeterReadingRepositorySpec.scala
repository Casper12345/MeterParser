package dao

import cats.effect.IO
import domain.MeterReading
import doobie.hikari.HikariTransactor
import munit.CatsEffectSuite
import util.DbUtil
import java.time.LocalDate

class MeterReadingRepositorySpec extends CatsEffectSuite {

  override def afterAll(): Unit = {
    DbUtil.truncateTable()
  }

  test("insert and get meter readings") {
    val xs = List(
      MeterReading("NEM1201015", LocalDate.parse("2023-01-01"), 10.0),
      MeterReading("NEM1201015", LocalDate.parse("2023-01-02"), 102.0),
      MeterReading("NEM1201015", LocalDate.parse("2023-01-03"), 112.0),
    )

    Transactor.transactor.use { xa =>
      given HikariTransactor[IO] = xa
      for {
        _ <- MeterReadingRepository.insertMeterReadings(xs)
        res <- DbUtil.getAllMeterReadings
      } yield assert(res == xs)
    }
  }
}
