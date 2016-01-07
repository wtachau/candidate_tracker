package com.willtachau.candidates
package persistence

import table.CustomPostgresDriver.api._
import model.core.{Id, WithId}
import model.core.Implicits.{idMapper}

import scala.concurrent.ExecutionContext.Implicits.global

trait DayRecordProvider {
  val dayRecordPersistence: DayRecord = new DayRecord
}

class DayRecord {
  private val dayRecordTable = TableQuery[table.DayRecord]

  def schema = dayRecordTable.schema

  def create(dayRecord: model.DayRecord) = {
    val id = dayRecordTable.returning(dayRecordTable.map(_.id)) += (WithId(Id("ignored"), dayRecord))
    id map { id =>
      WithId(id, dayRecord)
    }
  }

  def findAllRecords() = {
    dayRecordTable.result
  }

}
