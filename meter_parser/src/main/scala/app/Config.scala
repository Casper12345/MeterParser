package app

import com.typesafe.config.{Config as TsConfig, ConfigFactory}

/**
 * A configuration trait for injecting a config object. 
 * It ensures that the config is only loaded once but can be distributed wherever it is needed. 
 *
 * @tparam A the type of configuration to return
 */
trait Config[A] {
  val config: A
}

private object Conf {
  val config: TsConfig = ConfigFactory.load()
}

trait Conf extends Config[TsConfig] {
  val config: TsConfig = Conf.config
}
