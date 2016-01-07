package com.willtachau.candidates
package api

trait RealEnv extends util.RootConfig
  with util.CandidatesConfig
  with util.DatabaseProvider
  with persistence.DayRecordProvider
  with persistence.CandidateRecordProvider
  with service.DayRecordProvider
  with service.CandidateRecordProvider {
  self: util.CandidatesConfig =>
}