name := "RedisQueue"

version := "0.1"

scalaVersion := "2.12.8" 

val akkaVersion = "2.5.16"
val akkaHttpVersion  = "10.1.5"

libraryDependencies ++= Seq (
  "com.typesafe.akka"      %% "akka-actor"           % akkaVersion,
  "com.github.etaty"       %% "rediscala"            % "1.8.0",
  "io.spray"               %% "spray-json"           % "1.3.3",
  "com.typesafe.akka"      %% "akka-http"            % akkaHttpVersion,
  "com.typesafe.akka"      %% "akka-http-spray-json" % akkaHttpVersion
)