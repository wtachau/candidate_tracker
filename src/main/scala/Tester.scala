import io.indico.Indico
import io.indico.api.results.IndicoResult
import io.indico.api.results.BatchIndicoResult
import akka.stream.actor.ActorPublisher

import scala.collection.mutable.ArrayBuffer
import scala.util._


import collection.JavaConversions._
import akka.http._
import akka.stream.scaladsl._

import scala.concurrent.ExecutionContext
import de.heikoseeberger.akkasse.{EventStreamMarshalling, ServerSentEvent, WithHeartbeats}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives
import java.time.format.DateTimeFormatter
import java.time.LocalTime
import scala.concurrent.duration.DurationInt
import akka.stream.{ActorMaterializer, Materializer}

import akka.stream.scaladsl.{Source, Flow, Sink}
import twitter4j._
import twitter4j.conf.ConfigurationBuilder
import akka.actor._


object Candidate extends Enumeration {
  type Candidate = Value
  val Trump, Clinton, Bernie = Value
}

case class AnalyzedTweet(
  sentimentScore: Double,
  status: Status,
  candidates: List[Candidate.Value]
)

object TweetAnalyzer {

  val INDICO_API_KEY = "4393afa4379156e649a3bed6937328c2"
  val indico = new Indico(INDICO_API_KEY)

  val clintonKeywords = List("Clinton")
  val trumpKeywords = List("Trump")
  val bernieKeywords = List("Bernie", "FeelTheBern", "Sanders")

  var latestTweets : Map[Candidate.Value, Status] = Map()

  var clintonAverage:Double = 0
  var trumpAverage:Double = 0
  var bernieAverage:Double = 0

  var clintonTotal = 0
  var trumpTotal = 0
  var bernieTotal = 0

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
    if (textInStatus(clintonKeywords.map(x => x.toLowerCase))) {
      candidateList = candidateList :+ Candidate.Clinton
    }
    if (textInStatus(trumpKeywords.map(x => x.toLowerCase))) {
      candidateList = candidateList :+ Candidate.Trump
    }
    if (textInStatus(bernieKeywords.map(x => x.toLowerCase))) {
      candidateList = candidateList :+ Candidate.Bernie
    }
    return candidateList
  }

  def weightedAverage(currentAverage:Double, currentTotal:Int, recentTweets:Array[AnalyzedTweet], theCandidate:Candidate.Value) : (Int, Double) = {

    val candidateTweets = recentTweets.filter{ tweet => tweet.candidates.contains(theCandidate)}
    val recentTotal = candidateTweets.length
    val recentAverage = if (recentTotal > 0 ) candidateTweets.map{tweet => tweet.sentimentScore}.sum / recentTotal else 0

    val newTotal = recentTotal + currentTotal

    println(s"Candidate: $theCandidate, currentTotal: $currentTotal, recentTotal: $recentTotal, recentAverage: $recentAverage")

    val average = if (newTotal > 0) (currentTotal * currentAverage + recentTotal * recentAverage) / newTotal else 0

    return (newTotal, average)

  }

  def analyzeStatuses(statuses:Array[Status]) = {
    val result : BatchIndicoResult = indico.sentiment.predict(statuses.map(f => f.getText()))
    if (result.getSentiment() != null) {
      val sentimentScores = result.getSentiment().toList
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
      }


      // Calculate new running total/ averages
      val(newClintonTotal, newClintonAverage) = weightedAverage(clintonAverage, clintonTotal, analyzedTweets, Candidate.Clinton)
      val(newTrumpTotal, newTrumpAverage) = weightedAverage(trumpAverage, trumpTotal, analyzedTweets, Candidate.Trump)
      val (newBernieTotal, newBernieAverage) = weightedAverage(bernieAverage, bernieTotal, analyzedTweets, Candidate.Bernie)

      clintonAverage = newClintonAverage
      clintonTotal = newClintonTotal

      trumpAverage = newTrumpAverage
      trumpTotal = newTrumpTotal

      bernieAverage = newBernieAverage
      bernieTotal = newBernieTotal


      println(s"running clinton total: $clintonTotal")
      println(s"running trump total: $trumpTotal")
      println(s"running bernie total: $bernieTotal")

      println(s"****** >>>>>>>> rrunning clinton average: $clintonAverage")
      println(s"****** >>>>>>>> rrunning trump average: $trumpAverage")
      println(s"****** >>>>>>>> rrunning bernie average: $bernieAverage")


    } else {
      println("null result")
    }
  }

}

class TwitterStatusListener() extends StatusListener {

  var statusList = List[Status]()

  override def onDeletionNotice(statusDeletionNotice: StatusDeletionNotice): Unit = println("1"+statusDeletionNotice)

  override def onScrubGeo(userId: Long, upToStatusId: Long): Unit = println("2"+userId)

  override def onStallWarning(warning: StallWarning): Unit = println("3"+warning.toString())

  override def onStatus(status: Status): Unit = {

    statusList = statusList :+ status
    val batchSize = 15

    if (statusList.length >= batchSize) {
      println("analyzing...")
      TweetAnalyzer.analyzeStatuses(statusList.toArray)
      statusList = List.empty
    }
  }

  override def onTrackLimitationNotice(numberOfLimitedStatuses: Int): Unit = {
    println("TrackLimitationNotice: "+numberOfLimitedStatuses.toString())
  }

  override def onException(ex: Exception): Unit = ex.printStackTrace()

}

object ScalaTwitterClientExample {

  val clintonKeywords = List("Clinton")
  val trumpKeywords = List("Trump")
  val bernieKeywords = List("Bernie", "FeelTheBern")

  def main(args : Array[String]) {



    implicit val system: ActorSystem = ActorSystem("presidents")

    implicit val materializer = ActorMaterializer()(system)

    val cb = new ConfigurationBuilder
    cb.setDebugEnabled(true)
      .setOAuthConsumerKey("IVdfHnyhgvglsJ46CJpy3sz9E")
      .setOAuthConsumerSecret("BEYd4fY7etrVCWyY7dPcTVmB0Jz7hnFf5Hd0LpZRbP75ukx0IZ")
      .setOAuthAccessToken("34795132-Zl70Rtk5WCv66cnMdsAprGT4ocsltDOaCCXVLzLO1")
      .setOAuthAccessTokenSecret("BDG4kjW9VIbBrvnKd9O13q5SIX4KfdpFr8gU5gFsCccCC")

    val twitterStreamer = new TwitterStreamFactory(cb.build).getInstance()
    val statusListener = new TwitterStatusListener()
    twitterStreamer.addListener(statusListener)

    val keywords : List[String] = List(clintonKeywords, trumpKeywords, bernieKeywords).flatten
    twitterStreamer.filter(new FilterQuery(keywords:_*))

    Thread.sleep(20000)
    twitterStreamer.cleanUp()
    twitterStreamer.shutdown()
  }

//  def main(args: Array[String]) : Unit = {
//    implicit val system = ActorSystem()
//    implicit val mat = ActorMaterializer()
//    import system.dispatcher
//    Http().bindAndHandle(route(system), "127.0.0.1", 9000)
//
//    def route(system: ActorSystem)(implicit ec: ExecutionContext, mat: Materializer) = {
//      import Directives._
//      import EventStreamMarshalling._
//      get {
//        complete {
//          Source.tick(2.seconds, 2.seconds, ())
//            .map(_ => LocalTime.now())
//            .map(dateTimeToServerSentEvent)
//            .via(WithHeartbeats(1.second))
//        }
//      }
//    }
//
//    def dateTimeToServerSentEvent(time: LocalTime): ServerSentEvent = ServerSentEvent(
//      DateTimeFormatter.ISO_LOCAL_TIME.format(time)
//    )
//  }

}