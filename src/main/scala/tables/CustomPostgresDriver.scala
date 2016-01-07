package com.willtachau.candidates
package table

import com.github.tminglei.slickpg._
import slick.driver.PostgresDriver

import model.core.Id

trait CustomPostgresDriver extends PostgresDriver
with PgArraySupport
with PgDate2Support {

  override val api = new API {}

  trait API extends super.API
  with ArrayImplicits
  with DateTimeImplicits {
    implicit def idListTypeMapper[T: scala.reflect.ClassTag]: BaseColumnType[List[Id[T]]] = new SimpleArrayJdbcType[Id[T]]("text").to(_.toList)
  }
}

object CustomPostgresDriver extends CustomPostgresDriver
