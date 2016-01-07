package com.willtachau.candidates
package model

/**
  * Created by williamtachau on 12/11/15.
  */

import com.willtachau.candidates.api.Candidate

case class CandidateRecord(
  candidate: Candidate.Value,
  average: Double,
  total: Int
)
