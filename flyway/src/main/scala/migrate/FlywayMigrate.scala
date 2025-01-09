package migrate

import com.typesafe.config.ConfigFactory
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.FluentConfiguration
import org.flywaydb.core.api.output.MigrateResult

object FlywayMigrate {
  private val flyway = Configuration.getFlywayConfig.load()

  def migrate: MigrateResult = flyway.migrate()

}

object Main extends App {
  FlywayMigrate.migrate
}

private object Configuration {
  private val config = ConfigFactory.load()
  private val host = config.getString("db.host")
  private val port = config.getInt("db.port")
  private val database = config.getString("db.database")
  private val user = config.getString("db.user")
  private val password = config.getString("db.password")
  private val url = s"jdbc:postgresql://$host:$port/$database"
  private val location = config.getString("db.file_location")

  def getFlywayConfig: FluentConfiguration =
    Flyway.configure()
      .dataSource(
        url,
        user,
        password,
      ).locations(s"classpath:$location")
}