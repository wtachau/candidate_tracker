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
        path("pastTweets") {
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

                    // Define how we get to the tuple from our CandidateRecord Set
                    def asTriple(aSet:Set[Option[WithId[model.CandidateRecord]]]): Tuple3[Option[WithId[model.CandidateRecord]], Option[WithId[model.CandidateRecord]], Option[WithId[model.CandidateRecord]]] = {
                      return (aSet.head, aSet.tail.head, aSet.last)
                    }
                    // Split up
                    def unpackTotalAndAverage(list:List[Option[WithId[model.CandidateRecord]]]): Tuple2[Int, Double] = {
                      list.foldLeft((0,0.0))((a:Tuple2[Int, Double], b) => {
                        val newTotal = a._1 + b.get.data.total
                        val newAverage: Double = newTotal match {
                          case 0 => 0.0
                          case i: Int =>
                            logger.info(s"\n\n$a, $b")
//                            val one = a._2 * a._1
//                            val two = b.get.data.total * b.get.data.average
//                            val three = ((a._2 * a._1 + b.get.data.total * b.get.data.average) / i)
//                            print(s"\na._2 * a._1 = $one")
//                            print(s"\nb.get.data.total * b.get.data.average = $two")
//                            print(s"\n((a._2 * a._1 + b.get.data.total * b.get.data.average) / i) = $three")
                            ((a._2 * a._1 + b.get.data.total * b.get.data.average) / i)

                        }
                        (newTotal, newAverage)
                      })
                    }

                    val result = Await.result(futureDayRecords, 20 seconds).toList.unzip3(asTriple)
                    val unpacked = result match {
                      case (bernieList, clintonList, trumpList) =>
                        (unpackTotalAndAverage(bernieList), unpackTotalAndAverage(clintonList), unpackTotalAndAverage(trumpList))
                    }

                    PlayJson.obj(

                      s"$day" -> PlayJson.obj(
                        "bernie" -> PlayJson.obj(
                          "total" -> unpacked._1._1,
                          "average" -> unpacked._1._2
                        ),
                        "clinton" -> PlayJson.obj(
                          "total" -> unpacked._2._1,
                          "average" -> unpacked._2._2
                        ),
                        "trump" -> PlayJson.obj(
                          "total" -> unpacked._3._1,
                          "average" -> unpacked._3._2
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
        "total" -> TweetAnalyzer.clintonTotal,
        "date" -> "8-Apr-12"
      ),
      "trump" -> Map(
        "average" -> TweetAnalyzer.trumpAverage,
        "total" -> TweetAnalyzer.trumpTotal,
        "date" -> "8-Apr-12"
      ),
      "bernie" -> Map(
        "average" -> TweetAnalyzer.bernieAverage,
        "total" -> TweetAnalyzer.bernieTotal,
        "date" -> "8-Apr-12"
      )).toJson.toString()
}
