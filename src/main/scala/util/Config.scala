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

trait DatabaseConfig {
  self: RootConfig =>

  lazy val databaseUrl = rootConfig.getString("candidates.db.baseUrl")
  lazy val databaseUser = rootConfig.getString("candidates.db.user")
  lazy val databasePassword = rootConfig.getString("candidates.db.password")

}