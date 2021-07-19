name := "learn-akka"

version := "0.1"

scalaVersion := "2.13.6"

val akkaVersion = "2.6.15"

libraryDependencies ++= Seq(
  // https://mvnrepository.com/artifact/com.typesafe.akka/akka-actor
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,

  // https://mvnrepository.com/artifact/com.typesafe.akka/akka-testkit
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,

    // https://mvnrepository.com/artifact/org.scalatest/scalatest
    "org.scalatest" %% "scalatest" % "3.2.9",

)
