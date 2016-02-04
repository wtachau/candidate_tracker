package com.willtachau.candidates
package api

import com.typesafe.scalalogging.StrictLogging

import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.directives.{RespondWithDirectives, RouteDirectives, PathDirectives, MethodDirectives}
import akka.stream.Materializer
import akka.http.scaladsl.model.HttpHeader
import com.willtachau.candidates.model.core.WithId
import com.willtachau.candidates.model.{CandidateRecord, DayRecord}
import spray.http.HttpHeaders.RawHeader

import scala.collection.mutable.ListBuffer
import scala.concurrent.{Await, Future}

//import spray.http.HttpHeaders.RawHeader
import spray.routing._
import akka.stream.scaladsl.{Source}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import spray.json._
import M2eeJsonProtocol._
import de.heikoseeberger.akkasse.{EventStreamMarshalling, ServerSentEvent, WithHeartbeats}
import scala.util.{Random, Success, Failure}
import rapture.json._
import persistence.CandidateRecord
import play.api.libs.json.{Json => PlayJson, JsObject => PlayJsObject}

/**
  * Created by williamtachau on 12/14/15.
  */
trait TweetHttpService extends HttpService
  with StrictLogging {

  val dayRecordService: service.DayRecord
  val candidateRecordService: service.CandidateRecord

  val tweetRoutes = {
    respondWithHeader(RawHeader("Access-Control-Allow-Origin", "*")) {
      pathPrefix("api") {
        path ("liveTweets") {
          get {
            complete {
              latestTweetInfo()
            }
          }
        } ~
        path("recentTweets") {
          parameters("candidate") { candidate =>
            get {
              complete {
                candidate match {
                  case "bernie" =>
                    Map(
                      "id" -> s"${RecentStatusRecorder.recentBernieStatus.getId}",
                      "user" -> RecentStatusRecorder.recentBernieStatus.getUser.getScreenName
                    ).toJson.toString
                  case "clinton" =>
                    Map(
                      "id" -> s"${RecentStatusRecorder.recentClintonStatus.getId}",
                      "user" -> RecentStatusRecorder.recentClintonStatus.getUser.getScreenName
                    ).toJson.toString
                  case "trump" =>
                    Map(
                      "id" -> s"${RecentStatusRecorder.recentTrumpStatus.getId}",
                      "user" -> RecentStatusRecorder.recentTrumpStatus.getUser.getScreenName
                    ).toJson.toString
                  case "cruz" =>
                    Map(
                      "id" -> s"${RecentStatusRecorder.recentCruzStatus.getId}",
                      "user" -> RecentStatusRecorder.recentCruzStatus.getUser.getScreenName
                    ).toJson.toString
                  case "rubio" =>
                    Map(
                      "id" -> s"${RecentStatusRecorder.recentRubioStatus.getId}",
                      "user" -> RecentStatusRecorder.recentRubioStatus.getUser.getScreenName
                    ).toJson.toString
                  case _ => "candidate not found"
                }
              }
            }
          }
        } ~
        path("pastTweets.json") {
          get {
            onComplete(dayRecordService.getAllRecords()) {
              case Success(records) =>
                // Group records by the day
                val recordDayMaps = records.groupBy(record =>
                  record.data.date.getDayOfYear).map {
                  case (day, recordGroup) =>
                    val futureRecords: Seq[Set[Future[Option[WithId[model.CandidateRecord]]]]] = recordGroup map {
                      dayRecord => dayRecord.data.candidateRecords.map {
                        candidateRecordID => candidateRecordService.findById(candidateRecordID)
                      }
                    }
                    // Do weird future sequencing stuff
                    val futureCandidateRecords: Seq[Future[Set[Option[WithId[model.CandidateRecord]]]]] = futureRecords.map {
                      candidateRecords => Future.sequence(candidateRecords)
                    }
                    val futureDayRecords: Future[Seq[Set[Option[WithId[model.CandidateRecord]]]]] = Future.sequence(futureCandidateRecords)

                    // Split up
                    def unpackTotalAndAverage(list:List[WithId[model.CandidateRecord]]): Tuple2[Int, Double] = {
                      list.foldLeft((0,0.0))((a:Tuple2[Int, Double], b) => {
                        val newTotal = a._1 + b.data.total
                        val newAverage: Double = newTotal match {
                          case 0 => 0.0
                          case i: Int =>
                            logger.info(s"\n\n$a, $b")
                            ((a._2 * a._1 + b.data.total * b.data.average) / i)

                        }
                        (newTotal, newAverage)
                      })
                    }

                    var bernieRecords = new ListBuffer[WithId[model.CandidateRecord]]
                    var trumpRecords = new ListBuffer[WithId[model.CandidateRecord]]
                    var clintonRecords= new ListBuffer[WithId[model.CandidateRecord]]
                    var cruzRecords = new ListBuffer[WithId[model.CandidateRecord]]
                    var rubioRecords = new ListBuffer[WithId[model.CandidateRecord]]

                    Await.result(futureDayRecords, 20 seconds).toList.map { set =>
                      set map { recordOption =>
                        recordOption map { record =>
                          record.data.candidate match {
                            case Candidate.Bernie =>
                              bernieRecords += record
                            case Candidate.Trump =>
                              trumpRecords += record
                            case Candidate.Clinton =>
                              clintonRecords += record
                            case Candidate.Cruz =>
                              cruzRecords += record
                            case Candidate.Rubio =>
                              rubioRecords += record
                          }
                        }
                      }
                    }

                    val (bernieTotal, bernieAverage) = unpackTotalAndAverage(bernieRecords.toList)
                    val (trumpTotal, trumpAverage) = unpackTotalAndAverage(trumpRecords.toList)
                    val (clintonTotal, clintonAverage) = unpackTotalAndAverage(clintonRecords.toList)
                    val (cruzTotal, cruzAverage) = unpackTotalAndAverage(cruzRecords.toList)
                    val (rubioTotal, rubioAverage) = unpackTotalAndAverage(rubioRecords.toList)

                    PlayJson.obj(

                      s"$day" -> PlayJson.obj(
                        "bernie" -> PlayJson.obj(
                          "total" -> bernieTotal,
                          "average" -> bernieAverage
                        ),
                        "clinton" -> PlayJson.obj(
                          "total" -> clintonTotal,
                          "average" -> clintonAverage
                        ),
                        "trump" -> PlayJson.obj(
                          "total" -> trumpTotal,
                          "average" -> trumpAverage
                        ),
                        "cruz" -> PlayJson.obj(
                          "total" -> cruzTotal,
                          "average" -> cruzAverage
                        ),
                        "rubio" -> PlayJson.obj(
                          "total" -> rubioTotal,
                          "average" -> rubioAverage
                        )
                      )
                    )
                }
                complete {
                  PlayJson.obj("dayRecords" -> recordDayMaps.toList).toString()
                }

              case Failure(ex) =>
                complete {
                  print(s">> Error getting record")
                  ex.printStackTrace()
                  "ruh roh"
                }
            }
          }
        }
      }
    }
  }

  def latestTweetInfo(): String = Map(
      "test" -> 3,
      "clinton" -> Map(
        "average" -> TweetAnalyzer.clintonAverage,
        "total" -> TweetAnalyzer.clintonTotal
      ),
      "trump" -> Map(
        "average" -> TweetAnalyzer.trumpAverage,
        "total" -> TweetAnalyzer.trumpTotal
      ),
      "bernie" -> Map(
        "average" -> TweetAnalyzer.bernieAverage,
        "total" -> TweetAnalyzer.bernieTotal
      ),
      "cruz" -> Map(
        "average" -> TweetAnalyzer.cruzAverage,
        "total" -> TweetAnalyzer.cruzTotal
      ),
      "rubio" -> Map(
       "average" -> TweetAnalyzer.rubioAverage,
        "total" -> TweetAnalyzer.rubioTotal
      )
  ).toJson.toString()
}
