name := "my_netty_proxy"

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "io.netty" % "netty-all" % "4.0.34.Final",
  "com.typesafe.akka" %% "akka-actor" % "2.4.2"
)