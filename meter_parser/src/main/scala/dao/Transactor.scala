package dao

import app.Conf
import doobie.*
import doobie.postgres.implicits.*
import cats.effect.{IO, Resource}
import doobie.hikari.HikariTransactor
import java.util.concurrent.{Executors, ExecutorService}
import scala.concurrent.ExecutionContext

/**
 * Generic HikariTransactor for cats.effect.IO effect types returning a Resource.
 * It uses the virtual thread pool not to tie up OS threads.  
 */
object Transactor extends Conf {
  private val host = config.getString("db.host")
  private val port = config.getInt("db.port")
  private val database = config.getString("db.database")
  private val user = config.getString("db.user")
  private val password = config.getString("db.password")
  private val driver = config.getString("db.driver")
  private val url = s"jdbc:postgresql://$host:$port/$database"

  def transactor: Resource[IO, HikariTransactor[IO]] = {
    for {
      ce <- ThreadPool.virtualThreadPool
      xa <- HikariTransactor.newHikariTransactor[IO](
        driverClassName = driver,
        url = url,
        user = user,
        pass = password,
        connectEC = ce
      )
    } yield xa
  }
}

object ThreadPool {
  def virtualThreadPool: Resource[IO, ExecutionContext] =
    Resource.make(IO(Executors.newVirtualThreadPerTaskExecutor()))(
      (es: ExecutorService) => IO(es.shutdown())
    ).map(ExecutionContext.fromExecutor)
}