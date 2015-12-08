name := "foobar"

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies ++= {
  val akkaStreamVersion      = "2.0-M2"
  Seq(
    "com.typesafe.akka"   %% "akka-stream-experimental" % akkaStreamVersion,
    "org.twitter4j" % "twitter4j-core" % "4.0.4",
    "org.twitter4j" % "twitter4j-stream" % "4.0.4",
    "com.typesafe.akka" %% "akka-http-core-experimental" % akkaStreamVersion,
    "de.heikoseeberger" %% "akka-sse" % "1.3.0"
  )
}

resolvers ++= Seq(
  "hseeberger at bintray" at "http://dl.bintray.com/hseeberger/maven"
)