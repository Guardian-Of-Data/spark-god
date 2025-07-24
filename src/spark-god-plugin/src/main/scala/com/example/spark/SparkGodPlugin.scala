package org.apache.spark // 이렇게 package를 지정해야 SparkUI에 직접 접근할 수 있다. (접근제어자 issue)
import org.apache.spark.api.plugin.{DriverPlugin, ExecutorPlugin, PluginContext, SparkPlugin}
import org.apache.spark.scheduler.{SparkListener, SparkListenerApplicationStart, SparkListenerApplicationEnd, SparkListenerJobStart, SparkListenerJobEnd, SparkListenerTaskStart, SparkListenerTaskEnd}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.util.QueryExecutionListener
import org.apache.spark.sql.execution.QueryExecution
import org.apache.spark.ui.{SparkUI, SparkUITab, WebUIPage}

import scala.collection.mutable
import scala.collection.JavaConverters._
import java.lang.reflect.Method
import scala.xml.{Node, NodeSeq, Unparsed}

/**
 * Spark UI Plugin that logs application events and adds custom UI tab
 */
class SparkGodPlugin extends SparkPlugin {
  override def driverPlugin(): DriverPlugin = new SparkGodDriverPlugin()
  override def executorPlugin(): ExecutorPlugin = new SparkGodExecutorPlugin()
}

/**
 * Utility object for adding custom UI tabs
 */
object UITabUtils {
  def addCustomTab(ui: SparkUI): Unit = {
    // Check if Hello tab already exists
    val isTabAlreadyInstalled = ui.getTabs.exists(_.name == "SparkGod")
    if (isTabAlreadyInstalled) {
      println("SparkGod Plugin: SparkGod tab is already installed, skipping installation...")
      return
    }
    
    // Create custom tab
    val sparkGodTab = new SparkGodTab(ui)
    
    // Create custom page with proper path
    val sparkGodPage = new SparkGodPage(sparkGodTab)
    
    // Attach page to tab
    sparkGodTab.attachPage(sparkGodPage)
    
    // Attach tab to SparkUI
    ui.attachTab(sparkGodTab)
    
    println("SparkGod Plugin: Hello World tab attached successfully")
    println(s"SparkGod Plugin: Tab name: ${sparkGodTab.name}")
    println(s"SparkGod Plugin: Page class: ${sparkGodPage.getClass.getSimpleName}")
    println(s"SparkGod Plugin: Available tabs: ${ui.getTabs.map(_.name).mkString(", ")}")
    
    println("SparkGod Plugin: SparkGod tab attached successfully")
    println(s"SparkGod Plugin: Tab name: ${sparkGodTab.name}")
    println(s"SparkGod Plugin: Page class: ${sparkGodPage.getClass.getSimpleName}")
    println(s"SparkGod Plugin: Available tabs: ${ui.getTabs.map(_.name).mkString(", ")}")
  }
}

/**
 * Driver-side plugin that sets up listeners and UI tab
 */
class SparkGodDriverPlugin extends DriverPlugin {
  
  override def init(sc: SparkContext, ctx: PluginContext): java.util.Map[String, String] = {
    // Add Spark listener
    sc.addSparkListener(new CustomSparkListener())

    // Get SparkUI directly since we're in the same package
    val ui: SparkUI = sc.ui.get

    // 정적 파일 핸들러 등록 (최신)
    SparkGodJettyUtils.addStaticHandler(ui, "ui/assets", "/spark-god/assets")
    SparkGodJettyUtils.addStaticHandler(ui, "ui", "/spark-god") // index.html 등

    // Add SQL listener only if SparkSession already exists
    SparkSession.getActiveSession match {
      case Some(spark) =>
        try {
          spark.listenerManager.register(new CustomQueryExecutionListener())
          println("SparkGod Plugin: SQL listener registered successfully")
        } catch {
          case e: Exception =>
            println(s"SparkGod Plugin: Could not register SQL listener: ${e.getMessage}")
        }
      case None =>
        println("SparkGod Plugin: No active SparkSession found, skipping SQL listener registration")
    }

    // Add custom UI tab with delay to ensure it appears last
    try {
      // Use a separate thread to add the tab after a short delay
      new Thread(() => {
        Thread.sleep(1000) // Wait 1 second for other tabs to be added
        try {
          // Check if SparkContext is still active before adding tab
          val scOpt = SparkContext.getActive
          if (scOpt.isDefined && !scOpt.get.isStopped) {
            UITabUtils.addCustomTab(ui)
            println("SparkGod Plugin: Custom UI tab added successfully")
          } else {
            println("SparkGod Plugin: SparkContext is not available or stopped, skipping UI tab addition")
          }
        } catch {
          case e: Exception =>
            println(s"SparkGod Plugin: Could not add UI tab in thread: ${e.getMessage}")
        }
      }).start()
    } catch {
      case e: Exception =>
        println(s"SparkGod Plugin: Could not add UI tab: ${e.getMessage}")
        e.printStackTrace()
    }

    println("SparkGod Plugin: Custom listeners attached successfully")

    // Return empty map as required by the interface
    new java.util.HashMap[String, String]()
  }
  
  def addCustomTab(ui: SparkUI): Unit = {
    // Check if Hello tab already exists
    val isSparkGodTabAlreadyInstalled = ui.getTabs.exists(_.name == "SparkGod")
    if (isSparkGodTabAlreadyInstalled) {
      println("SparkGod Plugin: SparkGod tab is already installed, skipping installation...")
      return
    }
    
    // Create custom tab
    val sparkGodTab = new SparkGodTab(ui)
    
    // Create custom page
    val sparkGodPage = new SparkGodPage(sparkGodTab)
    
    // Attach page to tab
    sparkGodTab.attachPage(sparkGodPage)
    
    // Attach tab to SparkUI
    ui.attachTab(sparkGodTab)
    
    println("SparkGod Plugin: SparkGod tab attached successfully")
  }
  
  override def shutdown(): Unit = {
    println("SparkGod Plugin: Driver plugin shutdown")
  }
}

/**
 * Executor-side plugin (minimal implementation)
 */
class SparkGodExecutorPlugin extends ExecutorPlugin {
  
  override def init(ctx: PluginContext, extraConf: java.util.Map[String, String]): Unit = {
    println("SparkGod Plugin: Executor plugin initialized")
  }
  
  override def shutdown(): Unit = {
    println("SparkGod Plugin: Executor plugin shutdown")
  }
}

/**
 * Custom Spark Listener that logs application events
 */
class CustomSparkListener extends SparkListener {
  
  override def onApplicationStart(applicationStart: SparkListenerApplicationStart): Unit = {
    println(s"[PLUGIN] Application started: ${applicationStart.appName}")
    println(s"[PLUGIN] Application ID: ${applicationStart.appId}")
    println(s"[PLUGIN] Start time: ${applicationStart.time}")
    println(s"[PLUGIN] Check the Spark Web UI for the SparkGod tab!")
    
    // Add custom tab after application starts (ensures it appears last)
    try {
      val scOpt = SparkContext.getActive
      if (scOpt.isDefined && !scOpt.get.isStopped) {
        val ui = scOpt.get.ui.get
        UITabUtils.addCustomTab(ui)
        println("[PLUGIN] Custom UI tab added from application start event")
      } else {
        println("[PLUGIN] SparkContext is not available or stopped, skipping UI tab addition")
      }
    } catch {
      case e: Exception =>
        println(s"[PLUGIN] Could not add UI tab from application start: ${e.getMessage}")
    }
  }
  
  override def onApplicationEnd(applicationEnd: SparkListenerApplicationEnd): Unit = {
    println(s"[PLUGIN] Application ended at: ${applicationEnd.time}")
  }
  
  override def onJobStart(jobStart: SparkListenerJobStart): Unit = {
    println(s"[PLUGIN] Job ${jobStart.jobId} started with ${jobStart.stageIds.size} stages")
  }
  
  override def onJobEnd(jobEnd: SparkListenerJobEnd): Unit = {
    println(s"[PLUGIN] Job ${jobEnd.jobId} ended with result: ${jobEnd.jobResult}")
  }
  
  override def onTaskStart(taskStart: SparkListenerTaskStart): Unit = {
    println(s"[PLUGIN] Task ${taskStart.taskInfo.taskId} started in stage ${taskStart.stageId}")
  }
  
  override def onTaskEnd(taskEnd: SparkListenerTaskEnd): Unit = {
    val duration = taskEnd.taskInfo.finishTime - taskEnd.taskInfo.launchTime
    println(s"[PLUGIN] Task ${taskEnd.taskInfo.taskId} ended in ${duration}ms")
  }
}

/**
 * Custom Query Execution Listener that logs SQL queries
 */
class CustomQueryExecutionListener extends QueryExecutionListener {
  
  override def onSuccess(funcName: String, qe: QueryExecution, durationNs: Long): Unit = {
    val durationMs = durationNs / 1000000
    println(s"[PLUGIN] SQL query executed successfully in ${durationMs}ms")
  }
  
  override def onFailure(funcName: String, qe: QueryExecution, exception: Exception): Unit = {
    println(s"[PLUGIN] SQL query failed: ${exception.getMessage}")
  }
}

/** Custom Spark UI Tab */
class SparkGodTab(parent: SparkUI) extends SparkUITab(parent, "spark-god") {
  override val name = "SparkGod"
}
