scalacOptions ++= Seq("-deprecation","-unchecked")

resolvers += "Twitter Repository" at "http://maven.twttr.com/"

parallelExecution in Test := false

parallelExecution in IntegrationTest := false

libraryDependencies ++= Seq(
  "org.specs2" %% "specs2" % "1.7.1" % "test",
  "org.mockito" % "mockito-all" % "1.9.0" % "test",
  "com.google.guava" % "guava" % "11.0",
  "com.yammer.metrics" %% "metrics-scala" % "2.1.1",
  "mysql" % "mysql-connector-java" % "5.1.19",
  "com.h2database" % "h2" % "1.3.158"
)


