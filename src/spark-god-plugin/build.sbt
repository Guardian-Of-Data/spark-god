name := "spark-god-plugin"
version := "1.0.0"
scalaVersion := "2.12.18"

// Spark dependencies
libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "3.5.0" % "provided",
  "org.apache.spark" %% "spark-sql" % "3.5.0" % "provided",
  "org.apache.spark" %% "spark-hive" % "3.5.0" % "provided"
)

// Web dependencies for UI
libraryDependencies ++= Seq(
  "org.scalatra" %% "scalatra" % "2.8.2",
  "org.scalatra" %% "scalatra-json" % "2.8.2",
  "org.json4s" %% "json4s-jackson" % "4.0.6",
  "org.json4s" %% "json4s-core" % "4.0.6",
  "org.json4s" %% "json4s-ast" % "4.0.6",
  "javax.servlet" % "javax.servlet-api" % "4.0.1" % "provided"
)

// Assembly plugin for creating fat JAR
assembly / assemblyMergeStrategy := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}
