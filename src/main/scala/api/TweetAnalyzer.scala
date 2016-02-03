package com.willtachau.candidates
package api

import twitter4j.Status
import io.indico._
import io.indico.api.results._
import scala.util.Random
import collection.JavaConverters._

/**
  * Created by williamtachau on 12/8/15.
  */

case class AnalyzedTweet(
                          sentimentScore: Double,
                          status: Status,
                          candidates: List[Candidate.Value]
                        )

object TweetAnalyzer {

  val INDICO_API_KEY = "4393afa4379156e649a3bed6937328c2"
  val indico = new Indico(INDICO_API_KEY)

  var latestTweets : Map[Candidate.Value, Status] = Map()

  var clintonAverage: Double = 0
  var trumpAverage: Double = 0
  var bernieAverage: Double = 0
  var cruzAverage: Double = 0
  var rubioAverage: Double = 0

  var clintonTotal = 0
  var trumpTotal = 0
  var bernieTotal = 0
  var cruzTotal = 0
  var rubioTotal = 0

  def typeOfCandidate(status:Status) : List[Candidate.Value] = {

    val lowercaseStatusText = status.getText().toLowerCase()
    var lowercaseQuoteText:String = null
    if (status.getQuotedStatus() != null) {
      lowercaseQuoteText = status.getQuotedStatus().getText().toLowerCase()
    }

    def textInStatus(keywords: List[String]) : Boolean = {
      ((keywords exists(lowercaseStatusText.contains)) ||
        (lowercaseQuoteText != null && (keywords exists (lowercaseQuoteText.contains))))
    }

    var candidateList = List[Candidate.Value]()
    if (textInStatus(Candidate.clintonKeywords.map(x => x.toLowerCase))) {
      candidateList = candidateList :+ Candidate.Clinton
    }
    if (textInStatus(Candidate.trumpKeywords.map(x => x.toLowerCase))) {
      candidateList = candidateList :+ Candidate.Trump
    }
    if (textInStatus(Candidate.bernieKeywords.map(x => x.toLowerCase))) {
      candidateList = candidateList :+ Candidate.Bernie
    }
    if (textInStatus(Candidate.cruzKeywords.map(x => x.toLowerCase))) {
      candidateList = candidateList :+ Candidate.Cruz
    }
    if (textInStatus(Candidate.rubioKeywords.map(x => x.toLowerCase))) {
      candidateList = candidateList :+ Candidate.Rubio
    }

    return candidateList
  }

  def weightedAverage(currentAverage:Double, currentTotal:Int, recentTweets:Array[AnalyzedTweet], theCandidate:Candidate.Value) : (Int, Double) = {

    val candidateTweets = recentTweets.filter{ tweet => tweet.candidates.contains(theCandidate)}
    val recentTotal = candidateTweets.length
    val recentAverage = if (recentTotal > 0 ) candidateTweets.map{tweet => tweet.sentimentScore}.sum / recentTotal else 0
    val newTotal = recentTotal + currentTotal
    val average = if (newTotal > 0) (currentTotal * currentAverage + recentTotal * recentAverage) / newTotal else 0
    return (newTotal, average)
  }

  def analyzeStatuses(statuses:Array[Status]) = {

    print("\n\n>> analyze status called: \n\n")

    val testing = false

    var nonNull = true
    var sentimentScores = List.fill(150)(Random.nextDouble():java.lang.Double)
    if (!testing) {
      val result: BatchIndicoResult = indico.sentiment.predict(statuses.map(f => f.getText()))
      print(s"${result.getSentiment}")
      nonNull = (result.getSentiment() != null)
      if (nonNull) {
        sentimentScores = result.getSentiment().asScala.toList
      }
    }

    if (nonNull) {

      val analyzedTweets = statuses.zipWithIndex.map { case (status, index) =>
        AnalyzedTweet(sentimentScores(index), status = status, candidates = typeOfCandidate(status))
      }

      // Save latest tweets for all candidates (for web display)
      analyzedTweets.foreach{ analyzedTweet:AnalyzedTweet =>
        if (analyzedTweet.candidates.contains(Candidate.Clinton)) {
          latestTweets = latestTweets.updated(Candidate.Clinton, analyzedTweet.status)
        }
        if (analyzedTweet.candidates.contains(Candidate.Trump)) {
          latestTweets = latestTweets.updated(Candidate.Trump, analyzedTweet.status)
        }
        if (analyzedTweet.candidates.contains(Candidate.Bernie)) {
          latestTweets = latestTweets.updated(Candidate.Bernie, analyzedTweet.status)
        }
        if (analyzedTweet.candidates.contains(Candidate.Cruz)) {
          latestTweets = latestTweets.updated(Candidate.Cruz, analyzedTweet.status)
        }
        if (analyzedTweet.candidates.contains(Candidate.Rubio)) {
          latestTweets = latestTweets.updated(Candidate.Rubio, analyzedTweet.status)
        }
      }

      println(s"previous clinton total: $clintonTotal")
      println(s"previous trump total: $trumpTotal")
      println(s"previous bernie total: $bernieTotal")
      println(s"previous cruz total: $cruzTotal")
      println(s"previous rubio total: $rubioTotal")

      // Calculate new running total/ averages
      val (newClintonTotal, newClintonAverage) = weightedAverage(clintonAverage, clintonTotal, analyzedTweets, Candidate.Clinton)
      val (newTrumpTotal, newTrumpAverage) = weightedAverage(trumpAverage, trumpTotal, analyzedTweets, Candidate.Trump)
      val (newBernieTotal, newBernieAverage) = weightedAverage(bernieAverage, bernieTotal, analyzedTweets, Candidate.Bernie)
      val (newCruzTotal, newCruzAverage) = weightedAverage(cruzAverage, cruzTotal, analyzedTweets, Candidate.Cruz)
      val (newRubioTotal, newRubioAverage) = weightedAverage(rubioAverage, rubioTotal, analyzedTweets, Candidate.Rubio)

      clintonAverage = newClintonAverage
      clintonTotal = newClintonTotal

      trumpAverage = newTrumpAverage
      trumpTotal = newTrumpTotal

      bernieAverage = newBernieAverage
      bernieTotal = newBernieTotal

      cruzAverage = newCruzAverage
      cruzTotal = newCruzTotal

      rubioAverage = newRubioAverage
      rubioTotal = newRubioTotal

      if (testing) {
        bernieAverage *= 1.01
        clintonAverage *= 1
        trumpAverage *= .99
      }

      println(s"running clinton total: $clintonTotal")
      println(s"running trump total: $trumpTotal")
      println(s"running bernie total: $bernieTotal")
      println(s"running cruz total: $cruzTotal")
      println(s"running rubio total: $rubioTotal")

      println(s"****** >>>>>>>> running clinton average: $clintonAverage")
      println(s"****** >>>>>>>> running trump average: $trumpAverage")
      println(s"****** >>>>>>>> running bernie average: $bernieAverage")
      println(s"****** >>>>>>>> running cruz average: $cruzAverage")
      println(s"****** >>>>>>>> running rubio average: $rubioAverage")

    } else {
      println("null result")
    }
  }

}
