package com.willtachau.candidates
package api

import akka.io.{IO, Tcp}
import akka.stream.ActorMaterializer
import com.willtachau.candidates.model.CandidateRecord
import com.willtachau.candidates.util.{Migration, DatabaseProvider, DBUtils}
import spray.can.Http
import twitter4j.{FilterQuery, TwitterStreamFactory}
import twitter4j.conf.ConfigurationBuilder
import akka.pattern.ask

import akka.actor.{Actor, ActorSystem, Props}
import akka.util.Timeout
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global


object Candidate extends Enumeration {
  type Candidate = Value
  val Trump, Clinton, Bernie, Cruz, Rubio = Value
  val clintonKeywords = List("Clinton")
  val trumpKeywords = List("Trump")
  val bernieKeywords = List("Bernie", "FeelTheBern", "Sanders")
  val cruzKeywords = List("cruz")
  val rubioKeywords = List("marco", "rubio")
}

class ApplicationActor() extends Actor
  with TweetHttpService
  with RealEnv {

  def actorRefFactory = context

  def receive = runRoute(
    logRequestResponse("all") {
      tweetRoutes
    }
  )
}

trait saving {
  self: DatabaseProvider =>

  val candidateRecordService: service.CandidateRecord
  val dayRecordService: service.DayRecord

}

class SaveActor() extends Actor
  with saving
  with RealEnv {

  def actorRefFactory = context

  def receive = {
    case "save" => {

      val BernieRecord = CandidateRecord(Candidate.Bernie, TweetAnalyzer.bernieAverage, TweetAnalyzer.bernieTotal)
      val ClintonRecord = CandidateRecord(Candidate.Clinton, TweetAnalyzer.clintonAverage, TweetAnalyzer.clintonTotal)
      val TrumpRecord = CandidateRecord(Candidate.Trump, TweetAnalyzer.trumpAverage, TweetAnalyzer.trumpTotal)
      val CruzRecord = CandidateRecord(Candidate.Cruz, TweetAnalyzer.cruzAverage, TweetAnalyzer.cruzTotal)
      val RubioRecord = CandidateRecord(Candidate.Rubio, TweetAnalyzer.rubioAverage, TweetAnalyzer.rubioTotal)

      for {
        bernieId <- candidateRecordService.save(BernieRecord)
        clintonId <- candidateRecordService.save(ClintonRecord)
        trumpId <- candidateRecordService.save(TrumpRecord)
        cruzId <- candidateRecordService.save(CruzRecord)
        rubioId <- candidateRecordService.save(RubioRecord)
      } yield dayRecordService.save(Set(bernieId.id, clintonId.id, trumpId.id, cruzId.id, rubioId.id))

      TweetAnalyzer.bernieAverage = 0
      TweetAnalyzer.bernieTotal = 0

      TweetAnalyzer.clintonAverage = 0
      TweetAnalyzer.clintonTotal = 0

      TweetAnalyzer.trumpAverage = 0
      TweetAnalyzer.trumpTotal = 0

      TweetAnalyzer.cruzAverage = 0
      TweetAnalyzer.cruzTotal = 0

      TweetAnalyzer.rubioAverage = 0
      TweetAnalyzer.rubioTotal = 0

      print("saving...")

      val bernTotal = TweetAnalyzer.bernieTotal
      println(s"\n (bern's total now: $bernTotal)")
    }
  }
}

import spray.routing.SimpleRoutingApp

object Boot extends App
  with util.CandidatesConfig
  with util.RootConfig
  with util.DatabaseProvider
  with Migration
  with SimpleRoutingApp {
    new DBUtils().appDBSetup()

//    migrate()

    implicit val system: ActorSystem = ActorSystem("presidents")
    implicit val materializer = ActorMaterializer()(system)

    val service = system.actorOf(Props(classOf[ApplicationActor]), "candidates-api")
    implicit val timeout = Timeout(5.seconds)

    IO(Http).ask(Http.Bind(service, interface = "0.0.0.0", port = 8080, 100, Nil, None)).flatMap {
      case b: Http.Bound ⇒ Future.successful(b)
      case Tcp.CommandFailed(b: Http.Bind) ⇒
        // TODO: replace by actual exception when Akka #3861 is fixed.
        //       see https://www.assembla.com/spaces/akka/tickets/3861
        Future.failed(new RuntimeException(
          "Binding failed. Switch on DEBUG-level logging for `akka.io.TcpListener` to log the cause."))
    }(system.dispatcher)

    // Every 52 seconds is about 50,000 times a month
    system.scheduler.schedule(0 seconds, 31 seconds)(RecentStatusRecorder.analyzeLatestStatus)

    val saveActor = system.actorOf(Props(classOf[SaveActor]), "saveactor")
    system.scheduler.schedule(0 seconds, 3600 seconds, saveActor, "save" )
//  system.scheduler.schedule(0 seconds, 54 seconds, saveActor, "save" )


    println("Listening...")

    val cb = new ConfigurationBuilder
    cb.setDebugEnabled(true)
      .setOAuthConsumerKey("IVdfHnyhgvglsJ46CJpy3sz9E")
      .setOAuthConsumerSecret("BEYd4fY7etrVCWyY7dPcTVmB0Jz7hnFf5Hd0LpZRbP75ukx0IZ")
      .setOAuthAccessToken("34795132-Zl70Rtk5WCv66cnMdsAprGT4ocsltDOaCCXVLzLO1")
      .setOAuthAccessTokenSecret("BDG4kjW9VIbBrvnKd9O13q5SIX4KfdpFr8gU5gFsCccCC")

    val twitterStreamer = new TwitterStreamFactory(cb.build).getInstance()
    val statusListener = new TwitterStatusListener()
    twitterStreamer.addListener(statusListener)

    val keywords: List[String] = List(
      Candidate.clintonKeywords,
      Candidate.trumpKeywords,
      Candidate.bernieKeywords,
      Candidate.cruzKeywords,
      Candidate.rubioKeywords).flatten
    twitterStreamer.filter(new FilterQuery(keywords: _*))
}