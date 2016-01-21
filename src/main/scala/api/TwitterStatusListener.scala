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

  override def onStatus(status: Status): Unit =
    RecentStatusRecorder.recentStatus = status

  override def onTrackLimitationNotice(numberOfLimitedStatuses: Int): Unit =
    logger.info("TrackLimitationNotice: "+numberOfLimitedStatuses.toString())

  override def onException(ex: Exception): Unit =
    ex.printStackTrace()
}

object RecentStatusRecorder {
  var recentStatus: Status = _

  def analyzeLatestStatus() = {
    val statusToAnalyze = recentStatus
    println("analyzing status:")
    if (statusToAnalyze != null) {
      println(statusToAnalyze.getText())
      TweetAnalyzer.analyzeStatuses(Array(statusToAnalyze))
    }
  }
}