name := "login"

version := "0.0.1"

organization := "com.github.nabezokodaikon"

libraryDependencies ++= {
  val scalaTestVersion = "3.0.5"
  val akkaVersion = "2.5.18"
  val akkaHttpVersion = "10.1.5"
  Seq(
    // Logger
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    // Test
    "org.scalactic" %% "scalactic" % scalaTestVersion,
    "org.scalatest" %% "scalatest" % scalaTestVersion % "test",
    "org.scalacheck" %% "scalacheck" % "1.14.0" % "test",
    // Akka
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
  )
}

initialCommands in console := "import login._"
