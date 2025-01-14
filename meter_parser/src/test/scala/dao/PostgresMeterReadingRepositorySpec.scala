package dao

import cats.effect.{IO, unsafe}
import domain.BaseMeterReading
import doobie.hikari.HikariTransactor
import munit.CatsEffectSuite
import util.DbUtil

import java.time.{LocalDate, LocalDateTime}

class PostgresMeterReadingRepositorySpec extends CatsEffectSuite {
  
  test("insert and get meter readings") {
    val xs = List(
      BaseMeterReading("NEM1201080", LocalDateTime.parse("2023-01-01T20:00"), 10.0),
      BaseMeterReading("NEM1201080", LocalDateTime.parse("2023-01-02T20:00"), 102.0),
      BaseMeterReading("NEM1201080", LocalDateTime.parse("2023-01-03T20:00"), 112.0),
    )
    

    Transactor.transactor.use { xa =>
      given HikariTransactor[IO] = xa
      val repo = new PostgresMeterReadingRepository(xa)
      for {
        _ <- repo.insertMeterReadings(xs)
        res <- DbUtil.getMeterReadings("NEM1201080")
        _<- DbUtil.cleanTable("NEM1201080")
      } yield assert(res == xs)
    }
  }
}
