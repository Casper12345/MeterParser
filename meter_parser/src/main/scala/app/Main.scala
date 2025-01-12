package app

import reader.{FileReader, Nem12FileReader}
import cats.effect.{IO, IOApp}
import fs2.Stream
import dao.Transactor
import doobie.hikari.HikariTransactor
import scala.language.postfixOps
import com.typesafe.scalalogging.LazyLogging

object Main extends IOApp.Simple with Conf with LazyLogging {

  private val directory = config.getString("app.file_dir")
  private val parallelism = config.getInt("app.parallelism")

  override def run: IO[Unit] = {
    Transactor.transactor.use { xa =>
      given HikariTransactor[IO] = xa

      for {
        _ <- IO(logger.info(s"Parser started"))
        files <- FileReader.listFiles(directory)
        _ <- IO(logger.info(s"Processing ${files.size} files"))
        _ <- Stream.emits(files)
          .parEvalMapUnordered(parallelism)(f =>
            Nem12FileReader().processFile(f).compile.drain.handleError(e =>
              logger.error(s"Error occurred while processing file: ${f.fileName}", e)
            )
          )
          .compile
          .drain
        _ <- IO(logger.info(s"Parsing complete"))
      } yield ()
    }
  }

}
