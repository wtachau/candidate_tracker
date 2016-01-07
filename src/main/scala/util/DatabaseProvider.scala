package com.willtachau.candidates
package util

/**
  * Created by williamtachau on 12/11/15.
  */

import table.CustomPostgresDriver.api.Database

trait DatabaseProvider {
  self: CandidatesConfig =>

  lazy val db: Database = Database.forConfig("db", candidatesConfig)
}
