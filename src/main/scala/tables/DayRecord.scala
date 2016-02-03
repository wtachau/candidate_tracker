package com.willtachau.candidates
package table

import java.time.OffsetDateTime

import model.core.{Id, WithId}
import model.core.Implicits.idMapper
import table.CustomPostgresDriver.api._

class DayRecord(tag: Tag) extends Table[WithId[model.DayRecord]](tag, "dayRecords") {
  def id = column[Id[model.DayRecord]]("id", O.PrimaryKey, O.AutoInc)
  def date = column[OffsetDateTime]("date")
  def candidateRecords = column[List[String]]("candidateRecords")

  def * = (id, date, candidateRecords) <> ((mapRow _).tupled, unmapRow _)

  private def mapRow(id: Id[model.DayRecord], date: OffsetDateTime, candidateRecords: List[String]) =
    WithId(
      id,
      model.DayRecord(
        date,
        candidateRecords.toSet.map(Id[model.CandidateRecord](_: String))
      )
    )

  private def unmapRow(dayRecordWithId: WithId[model.DayRecord]) =
    Some((
      dayRecordWithId.id,
      dayRecordWithId.data.date,
      dayRecordWithId.data.candidateRecords.toList.map(_.value.toString)
      ))
}