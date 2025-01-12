package dao

import cats.effect.{IO, unsafe}
import domain.BaseMeterReading
import doobie.hikari.HikariTransactor
import munit.CatsEffectSuite
import util.DbUtil

import java.time.LocalDate

class PostgresMeterReadingRepositorySpec extends CatsEffectSuite {
  
  test("insert and get meter readings") {
    val xs = List(
      BaseMeterReading("NEM1201080", LocalDate.parse("2023-01-01"), 10.0),
      BaseMeterReading("NEM1201080", LocalDate.parse("2023-01-02"), 102.0),
      BaseMeterReading("NEM1201080", LocalDate.parse("2023-01-03"), 112.0),
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
