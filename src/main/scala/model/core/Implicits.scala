package com.willtachau.candidates
package model.core

/**
  * Created by williamtachau on 12/11/15.
  */

import slick.driver.PostgresDriver.api._

object Implicits {
  implicit def idMapper[M] = MappedColumnType.base[Id[M], String](
    {
      _.value
    }, {
      Id[M](_)
    }
  )

}