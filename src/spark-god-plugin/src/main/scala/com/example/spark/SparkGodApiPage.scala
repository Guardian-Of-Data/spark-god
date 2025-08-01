package org.apache.spark

import org.apache.spark.internal.Logging
import org.apache.spark.ui.{SparkUI, WebUIPage}
import org.json4s.{Extraction, JObject, JValue}

import javax.servlet.http.HttpServletRequest
import scala.xml.Node

case class SparkGodApplicationInfo(
  id: String,
  name: String,
  startTime: Long,
  endTime: Long,
  duration: Long
)

case class SparkGodEnvironmentInfo(
  sparkVersion: String,
  javaVersion: String,
  scalaVersion: String,
  osInfo: String,
  pythonVersion: String
)

case class SparkGodData(
  hello: String,
  application: SparkGodApplicationInfo,
  environment: SparkGodEnvironmentInfo,
  timestamp: Long
)

class SparkGodApiPage(ui: SparkUI) extends WebUIPage("api") with Logging {
  
  override def renderJson(request: HttpServletRequest): JValue = {
    try {
      val appInfo = ui.store.applicationInfo()
      val envInfo = ui.store.environmentInfo()
      
      // Get the latest attempt for timing information
      val latestAttempt = appInfo.attempts.lastOption
      val startTime = latestAttempt.map(_.startTime.getTime).getOrElse(0L)
      val endTime = latestAttempt.map(_.endTime.getTime).getOrElse(0L)
      val duration = if (endTime > 0) endTime - startTime else 0L
      
      // Helper function to find property value
      def findProperty(props: Seq[(String, String)], key: String): String = {
        props.find(_._1 == key).map(_._2).getOrElse("N/A")
      }
      
      val osName = findProperty(envInfo.systemProperties, "os.name")
      val osVersion = findProperty(envInfo.systemProperties, "os.version")
      val pythonVersion = findProperty(envInfo.systemProperties, "python.version")
      
      // Get Spark version from system properties or use default
      val sparkVersion = org.apache.spark.SPARK_VERSION

      val sparkGodData = SparkGodData(
        hello = "Hello, World!",
        application = SparkGodApplicationInfo(
          id = appInfo.id,
          name = appInfo.name,
          startTime = startTime,
          endTime = endTime,
          duration = duration
        ),
        environment = SparkGodEnvironmentInfo(
          sparkVersion = sparkVersion,
          javaVersion = envInfo.runtime.javaVersion,
          scalaVersion = envInfo.runtime.scalaVersion,
          osInfo = s"$osName $osVersion",
          pythonVersion = pythonVersion
        ),
        timestamp = System.currentTimeMillis()
      )
      
      val jsonValue = Extraction.decompose(sparkGodData)(org.json4s.DefaultFormats)
      jsonValue
    }
    catch {
      case e: Throwable => {
        logError("failed to serve sparkgod api data", e)
        JObject()
      }
    }
  }

  override def render(request: HttpServletRequest): Seq[Node] = Seq[Node]()
} 