package org.apache.spark

import org.apache.spark.internal.Logging
import org.apache.spark.ui.{SparkUI, WebUIPage}
import org.json4s.{Extraction, JObject, JValue}

import javax.servlet.http.HttpServletRequest
import scala.xml.Node

case class SparkGodEnvironmentData(
  sparkProperties: Map[String, String],
  hadoopProperties: Map[String, String],
  timestamp: Long
)

class SparkGodConfigPage(ui: SparkUI) extends WebUIPage("config") with Logging {
  
  override def renderJson(request: HttpServletRequest): JValue = {
    try {
      // Spark Properties (Environment 페이지의 Spark Properties 섹션)
      val sparkProperties = SparkContext.getActive match {
        case Some(sc) => sc.conf.getAll.toMap
        case None => Map.empty[String, String]
      }
      
      // Hadoop Properties (Environment 페이지의 Hadoop Properties 섹션)
      val hadoopProperties = SparkContext.getActive match {
        case Some(sc) => 
          val conf = sc.hadoopConfiguration
          import scala.collection.mutable
          val result = mutable.Map[String, String]()
          val iterator = conf.iterator()
          while (iterator.hasNext) {
            val entry = iterator.next()
            result.put(entry.getKey, entry.getValue)
          }
          result.toMap
        case None => Map.empty[String, String]
      }

      val environmentData = SparkGodEnvironmentData(
        sparkProperties = sparkProperties,
        hadoopProperties = hadoopProperties,
        timestamp = System.currentTimeMillis()
      )
      
      val jsonValue = Extraction.decompose(environmentData)(org.json4s.DefaultFormats)
      jsonValue
    }
    catch {
      case e: Throwable => {
        logError("failed to serve sparkgod config data", e)
        JObject()
      }
    }
  }

  override def render(request: HttpServletRequest): Seq[Node] = Seq[Node]()
}