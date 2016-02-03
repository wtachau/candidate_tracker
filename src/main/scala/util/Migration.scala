package com.willtachau.candidates
package util

import util.{DatabaseConfig, RootConfig}
import org.flywaydb.core.Flyway

trait Migration extends RootConfig with util.DatabaseConfig {
  self: util.DatabaseConfig =>

  private val flyway = new Flyway()
  flyway.setDataSource(databaseUrl, databaseUser, databasePassword)

  def migrate() = {
    flyway.migrate()
  }

  def reloadSchema() = {
    flyway.clean()
    flyway.migrate()
  }

}
