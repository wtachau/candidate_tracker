package com.willtachau.candidates
package model

import java.time.OffsetDateTime
import model.core.{Id, WithId}
import rapture.json.Json

case class DayRecord(
  date: OffsetDateTime,
  candidateRecords: Set[Id[CandidateRecord]]
)

object DayRecord {
  def fromRecords(records: Set[Id[CandidateRecord]]) = {
    DayRecord(
      date = OffsetDateTime.now(),
      candidateRecords = records
    )
  }
}