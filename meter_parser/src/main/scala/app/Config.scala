package app

import com.typesafe.config.{Config, ConfigFactory}

object Conf {
  val config : Config = ConfigFactory.load()
}

trait Conf {
  val config: Config = Conf.config
}
