name := "CandidateTracker"

version := "1.0"

scalaVersion := "2.11.7"

mainClass := Some("com.willtachau.candidates.api.Boot")

libraryDependencies ++= {
  val akkaStreamVersion      = "2.0-M2"
  Seq(
    "com.typesafe.akka"   %% "akka-stream-experimental" % akkaStreamVersion,
    "org.twitter4j" % "twitter4j-core" % "4.0.4",
    "org.twitter4j" % "twitter4j-stream" % "4.0.4",
    "com.typesafe.akka" %% "akka-actor" % "2.3.9",
    "com.typesafe.akka" %% "akka-http-core-experimental" % akkaStreamVersion,
    "de.heikoseeberger" %% "akka-sse" % "1.3.0",
    "io.spray" %%  "spray-json" % "1.3.2",
    "io.spray" %% "spray-routing" % "1.3.3",
    "io.spray" %% "spray-can" % "1.3.3",
    "com.typesafe.slick" %% "slick" % "3.0.0",
    "com.zaxxer" % "HikariCP" % "2.4.1",
    "org.postgresql" % "postgresql" % "9.4-1201-jdbc4",
    "com.propensive" %% "rapture-json-jawn" % "2.0.0-M1",
    "com.github.tminglei" %% "slick-pg" % "0.9.1",
    "io.indico" % "indico" % "3.1.0",
    "com.typesafe.play" %% "play-json" % "2.3.4",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
    "org.flywaydb" %  "flyway-core"  % "3.2.1"
  )
}

resolvers ++= Seq(
  "hseeberger at bintray" at "http://dl.bintray.com/hseeberger/maven",
  "Spray repository" at "http://repo.spray.io",
  "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
  "Flyway" at "http://flywaydb.org/repo"
)