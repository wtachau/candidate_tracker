package com.willtachau.candidates
package persistence

import table.CustomPostgresDriver.api._
import model.core.{Id, WithId}
import model.core.Implicits.{idMapper}

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by williamtachau on 1/5/16.
  */
trait CandidateRecordProvider {
  val candidateRecordPersistence: CandidateRecord = new CandidateRecord
}

class CandidateRecord {
  private val candidateRecordTable = TableQuery[table.CandidateRecord]

  def schema = candidateRecordTable.schema

  def create(candidateRecord: model.CandidateRecord) = {
    val id = candidateRecordTable.returning(candidateRecordTable.map(_.id)) += (WithId(Id("ignored"), candidateRecord))
    id map { id =>
      WithId(id, candidateRecord)
    }
  }

  def findById(id: Id[model.CandidateRecord]) = {
    candidateRecordTable.filter(_.id === id).result.headOption
  }
}
