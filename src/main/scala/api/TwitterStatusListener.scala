package com.willtachau.candidates
package api

import twitter4j.{StallWarning, Status, StatusDeletionNotice, StatusListener}

/**
  * Created by williamtachau on 12/8/15.
  */
class TwitterStatusListener() extends StatusListener {

  var statusList = List[Status]()

  override def onDeletionNotice(statusDeletionNotice: StatusDeletionNotice): Unit = println("1"+statusDeletionNotice)

  override def onScrubGeo(userId: Long, upToStatusId: Long): Unit = println("2"+userId)

  override def onStallWarning(warning: StallWarning): Unit = println("3"+warning.toString())

  override def onStatus(status: Status): Unit = {

    RecentStatusRecorder.recentStatus = status
//    print("changing status...")

//    statusList = statusList :+ status
//    val batchSize = 15
//
//    if (statusList.length >= batchSize) {
//      println("analyzing...")
//      TweetAnalyzer.analyzeStatuses(statusList.toArray)
//      statusList = List.empty
//    }
  }

  override def onTrackLimitationNotice(numberOfLimitedStatuses: Int): Unit = {
    println("TrackLimitationNotice: "+numberOfLimitedStatuses.toString())
  }

  override def onException(ex: Exception): Unit = ex.printStackTrace()
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