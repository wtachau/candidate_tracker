package com.willtachau.candidates
package service

import com.willtachau.candidates.model.core.{WithId, Id}
import com.willtachau.candidates.util.DatabaseProvider
import table.CustomPostgresDriver.api.Database
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by williamtachau on 1/5/16.
  */
trait CandidateRecordProvider {
  self: DatabaseProvider
    with persistence.CandidateRecordProvider =>

  lazy val candidateRecordService = new CandidateRecord(
    db,
    candidateRecordPersistence
  )
}
class CandidateRecord(
                 db: Database,
                 candidateRecordPersistence: persistence.CandidateRecord
               ) {
  def save(record: model.CandidateRecord): Future[WithId[model.CandidateRecord]] = for {
    candidateRecord <- db.run(candidateRecordPersistence.create(record))
  } yield candidateRecord

  def findById(id: Id[model.CandidateRecord]): Future[Option[WithId[model.CandidateRecord]]] =
    db.run(candidateRecordPersistence.findById(id))
}