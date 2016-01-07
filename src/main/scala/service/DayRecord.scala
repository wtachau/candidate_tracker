package com.willtachau.candidates
package service

import com.willtachau.candidates.model.core.{WithId, Id}
import util.DatabaseProvider
import table.CustomPostgresDriver.api.Database
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait DayRecordProvider {
  self: DatabaseProvider
  with persistence.DayRecordProvider =>

  lazy val dayRecordService = new DayRecord(
    db,
    dayRecordPersistence
  )
}
class DayRecord(
  db: Database,
  dayRecordPersistence: persistence.DayRecord
) {
  def save(candidateRecords: Set[Id[model.CandidateRecord]]): Future[WithId[model.DayRecord]] = {
    val dayRecord = model.DayRecord.fromRecords(candidateRecords)
    for {
      dayRecordSaved <- db.run(dayRecordPersistence.create(dayRecord))
    } yield dayRecordSaved
  }

  def getAllRecords(): Future[Seq[WithId[model.DayRecord]]] = {
    db.run(dayRecordPersistence.findAllRecords)
  }
}