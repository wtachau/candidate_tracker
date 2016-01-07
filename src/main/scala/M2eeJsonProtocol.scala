package com.willtachau.candidates

/**
  * Created by williamtachau on 12/9/15.
  */

import com.willtachau.candidates.model.core.WithId
import spray.json._

object M2eeJsonProtocol extends DefaultJsonProtocol {

  implicit object MapJsonFormat extends JsonFormat[Map[String, Any]] { // 1
  def write(m: Map[String, Any]) = {
    JsObject(m.mapValues {
      case v: String => JsString(v)
      case v: Int => JsNumber(v)
      case v: Map[String, Any] => write(v)
      case v: Any => JsString(v.toString)
    })
  }

    def read(value: JsValue) = ???
  }

  implicit object ListJsonFormat extends JsonFormat[List[Any]] {
    def write(l: List[Any]) = {
      val convertedValues = l.map {
        case m: Map[String, Any] => ("list", MapJsonFormat.write(m).asJsObject())
      }
      JsObject(members = convertedValues)
    }

    def read(value: JsValue) = ???
  }

}