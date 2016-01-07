package com.willtachau.candidates
package table

import java.time.OffsetDateTime

import com.willtachau.candidates.api.Candidate
import model.core.{Id, WithId}
import model.core.Implicits.idMapper
import table.CustomPostgresDriver.api._

class CandidateRecord(tag: Tag) extends Table[WithId[model.CandidateRecord]](tag, "candidateRecords") {
  def id = column[Id[model.CandidateRecord]]("id", O.PrimaryKey, O.AutoInc)
  def candidate = column[String]("candidate")
  def average = column[Double]("average")
  def total = column[Int]("total")

  def * = (id, candidate, average, total) <> ((mapRow _).tupled, unmapRow _)

  private def mapRow(id: Id[model.CandidateRecord], candidate: String, average: Double, total: Int) =
    WithId(
      id,
      model.CandidateRecord(
        Candidate.withName(candidate),
        average,
        total
      )
    )

  private def unmapRow(candidateRecordWithID: WithId[model.CandidateRecord]) =
    Some((
      candidateRecordWithID.id,
      candidateRecordWithID.data.candidate.toString,
      candidateRecordWithID.data.average,
      candidateRecordWithID.data.total
      ))
}
