package com.willtachau.candidates
package util

/**
  * Created by williamtachau on 12/11/15.
  */

import org.postgresql.util.PSQLException
import table.CustomPostgresDriver.api._
import slick.jdbc.meta.MTable

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Try, Failure, Success}

class DBUtils extends RootConfig
  with CandidatesConfig
  with DatabaseProvider
  with persistence.DayRecordProvider
  with persistence.CandidateRecordProvider {

  lazy val psqlRootDB = Database.forURL(
    candidatesConfig.getString("db.baseUrl") + "/postgres?" +
      s"user=${candidatesConfig.getString("db.user")}&" +
      s"password=${candidatesConfig.getString("db.password")}"
  )

  val fullSchema =
    (
      dayRecordPersistence.schema ++
      candidateRecordPersistence.schema
      )

  private def safeName[T](name: String)(f: => T): T = {
    if (!name.contains('"')) {
      f
    } else {
      throw new Exception(s"""Database name can't contain a ", but did: $name""")
    }
  }

  def dbExists(name: String): Try[Boolean] = {
    
    Try {
      println(psqlRootDB)
      Await.result(
        psqlRootDB.run(
          sql"select datname from pg_database".as[String]
        ),
        30.seconds
      )
    } map { databases =>
      databases.contains(name)
    }
  }

  def createTables(): Unit = {
    Await.result(db.run(fullSchema.create), 1.minute)
    println("Tables successfully created.")
  }

  lazy val tables = Await.result(
    db.run(MTable.getTables), 10.seconds
  ) map (_.name.name)

  def hasTable(name: String) = tables.contains(name)

  def createDB(name: String): Unit = safeName(name) {
    dbExists(name) map { exists =>
      if (!exists) {
        Await.result(psqlRootDB.run(
          sqlu"""create database "#$name""""
        ), 10.seconds)
      }
    } match {
      case Success(result) => ()
      case Failure(ex: PSQLException) =>
        ex.getMessage match {
          case "ERROR: permission denied to create database" =>
            val username = candidatesConfig.getString("db.user")
            throw new Exception(
              s"The user ${username} got '${ex.getMessage}' when trying to create database '${name}'!"
            )
          case _ =>
            throw ex
        }
      case Failure(other) => {
        throw other
      }
    }
  }

  def appDBSetup(): Unit = {
    createDB(candidatesConfig.getString("db.dbName"))
    if(!hasTable("dayRecords")) {
      println("It appears that there are no app tables in your database. I'll try to create them...")
      createTables()
    }
  }
}

object DBSetupHelper extends App {
  new DBUtils().appDBSetup()
}
