package com.willtachau.candidates
package model.core

/**
  * Created by williamtachau on 12/11/15.
  */

case class WithId[A](id: Id[A], data: A)

object WithId {
  import scala.language.implicitConversions

  implicit def withId2Data[A](withId: WithId[A]): A = withId.data
}

