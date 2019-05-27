name := "RedisQueue"

version := "0.1"

scalaVersion := "2.12.8" 

val akkaVersion = "2.5.16"

resolvers ++= Seq(
  "rediscala" at "http://dl.bintray.com/etaty/maven",
)

libraryDependencies ++= Seq (
  "com.typesafe.akka"      %% "akka-actor"           % akkaVersion,
  "com.github.etaty"       %% "rediscala"            % "1.8.0",
  "com.typesafe"           %  "config"               % "1.3.1",
  "io.spray"               %% "spray-json"           % "1.3.3"
)