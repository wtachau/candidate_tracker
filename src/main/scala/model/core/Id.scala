package com.willtachau.candidates
package model.core

import _root_.rapture.json.Json
import _root_.rapture.json.JsonAst
import _root_.rapture.json._
import com.willtachau.candidates.model.core._
import rapture.json._

case class Id[A](value: IdVal)

object Id {
  // custom serializer here because of an issue in rapture-json when
  // dealing with automatic serializer derivation of types like
  // Option[T[U]]
  implicit def serializer[T](implicit ast: JsonAst): Serializer[Id[T], Json] = new Serializer[Id[T], Json] {
    def serialize(id: Id[T]) =
      ast.fromString(id.value)
  }

  implicit def idExtractor[T] = Json.extractor[String].map(Id[T](_))
}
