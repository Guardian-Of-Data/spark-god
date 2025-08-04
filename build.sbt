name := "spark-god"
version := "1.0.0"
scalaVersion := "2.12.18"

lazy val sparkGodPlugin = (project in file("src/spark-god-plugin"))
  .settings(
    name := "spark-god-plugin",
    version := "1.0.0",
    scalaVersion := "2.12.18",
    
    // Spark dependencies - provided scope like Dataflint
    libraryDependencies ++= Seq(
      "org.apache.spark" %% "spark-core" % "3.5.0" % "provided",
      "org.apache.spark" %% "spark-sql" % "3.5.0" % "provided"
    ),
    
    publish / skip := true
  )

lazy val testApp = (project in file("src/test-app"))
  .settings(
    name := "test-app",
    version := "1.0.0",
    scalaVersion := "2.12.18",
    
    // Spark dependencies - compile scope like Dataflint examples
    libraryDependencies ++= Seq(
      "org.apache.spark" %% "spark-core" % "3.5.0",
      "org.apache.spark" %% "spark-sql" % "3.5.0"
    ),
    
    publish / skip := true
  )
  .dependsOn(sparkGodPlugin)
