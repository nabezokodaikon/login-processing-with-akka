name := "login"

version := "0.0.1"

organization := "com.github.nabezokodaikon"

libraryDependencies ++= {
  val scalaTestVersion = "3.0.5"
  Seq(
    // Logger
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2",
    // Test
    "org.scalactic" %% "scalactic" % scalaTestVersion,
    "org.scalatest" %% "scalatest" % scalaTestVersion % "test",
    "org.scalacheck" %% "scalacheck" % "1.14.0" % "test",
  )
}

initialCommands in console := "import login._"
