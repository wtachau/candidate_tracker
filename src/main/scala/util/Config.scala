package com.willtachau.candidates
package util

import com.typesafe.config.{Config, ConfigFactory}

trait RootConfig {
  lazy val rootConfig = ConfigFactory.load()
}

trait CandidatesConfig {
  self: RootConfig =>

  lazy val candidatesConfig: Config = rootConfig.getConfig("candidates")
}
