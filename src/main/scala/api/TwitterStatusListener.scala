package com.willtachau.candidates
package api

import com.typesafe.scalalogging.StrictLogging
import twitter4j.{StallWarning, Status, StatusDeletionNotice, StatusListener}

/**
  * Created by williamtachau on 12/8/15.
  */
class TwitterStatusListener() extends StatusListener
  with StrictLogging {

  var statusList = List[Status]()

  override def onDeletionNotice(statusDeletionNotice: StatusDeletionNotice): Unit =
    logger.info("onDeletionNotice: "+statusDeletionNotice)

  override def onScrubGeo(userId: Long, upToStatusId: Long): Unit =
    logger.info("onScrubGeo: "+userId)

  override def onStallWarning(warning: StallWarning): Unit =
    logger.info("onStallWarning: "+warning.toString())

  override def onStatus(status: Status): Unit = {

    RecentStatusRecorder.recentStatus = status

    val candidate = TweetAnalyzer.typeOfCandidate(status)
    if (!candidate.isEmpty) {
      candidate.head match {
        case Candidate.Bernie => RecentStatusRecorder.recentBernieStatus = status
        case Candidate.Clinton => RecentStatusRecorder.recentClintonStatus = status
        case Candidate.Trump => RecentStatusRecorder.recentTrumpStatus = status
        case _ => "no candidate found in tweet"
      }
    }
  }

  override def onTrackLimitationNotice(numberOfLimitedStatuses: Int): Unit =
    logger.info("TrackLimitationNotice: "+numberOfLimitedStatuses.toString())

  override def onException(ex: Exception): Unit =
    ex.printStackTrace()
}

object RecentStatusRecorder {
  var recentStatus: Status = _

  var recentBernieStatus: Status = _
  var recentClintonStatus: Status = _
  var recentTrumpStatus: Status = _

  def analyzeLatestStatus() = {
    val statusToAnalyze = recentStatus

    println("analyzing status:")
    if (statusToAnalyze != null) {
      println(statusToAnalyze.getText())
      TweetAnalyzer.analyzeStatuses(Array(statusToAnalyze))
    }
  }
}