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
class SparkUIPlugin extends SparkPlugin {
  override def driverPlugin(): DriverPlugin = new SparkUIDriverPlugin()
  override def executorPlugin(): ExecutorPlugin = new SparkUIExecutorPlugin()
}

/**
 * Utility object for adding custom UI tabs
 */
object UITabUtils {
  def addCustomUITab(ui: SparkUI): Unit = {
    // Check if Hello tab already exists
    val isHelloTabAlreadyInstalled = ui.getTabs.exists(_.name == "Hello")
    if (isHelloTabAlreadyInstalled) {
      println("Spark UI Plugin: Hello tab is already installed, skipping installation...")
      return
    }
    
    // Create custom tab
    val helloTab = new HelloWorldTab(ui)
    
    // Create custom page with proper path
    val helloPage = new HelloWorldPage(helloTab)
    
    // Attach page to tab
    helloTab.attachPage(helloPage)
    
    // Attach tab to SparkUI
    ui.attachTab(helloTab)
    
    println("Spark UI Plugin: Hello World tab attached successfully")
    println(s"Spark UI Plugin: Tab name: ${helloTab.name}")
    println(s"Spark UI Plugin: Page class: ${helloPage.getClass.getSimpleName}")
    println(s"Spark UI Plugin: Available tabs: ${ui.getTabs.map(_.name).mkString(", ")}")
    
    println("Spark UI Plugin: Hello World tab attached successfully")
    println(s"Spark UI Plugin: Tab name: ${helloTab.name}")
    println(s"Spark UI Plugin: Page class: ${helloPage.getClass.getSimpleName}")
    println(s"Spark UI Plugin: Available tabs: ${ui.getTabs.map(_.name).mkString(", ")}")
  }
}

/**
 * Driver-side plugin that sets up listeners and UI tab
 */
class SparkUIDriverPlugin extends DriverPlugin {
  
  override def init(sc: SparkContext, ctx: PluginContext): java.util.Map[String, String] = {
    // Add Spark listener
    sc.addSparkListener(new CustomSparkListener())
    
    // Get SparkUI directly since we're in the same package
    val ui: SparkUI = sc.ui.get

    // Add SQL listener only if SparkSession already exists
    SparkSession.getActiveSession match {
      case Some(spark) =>
        try {
          spark.listenerManager.register(new CustomQueryExecutionListener())
          println("Spark UI Plugin: SQL listener registered successfully")
        } catch {
          case e: Exception =>
            println(s"Spark UI Plugin: Could not register SQL listener: ${e.getMessage}")
        }
      case None =>
        println("Spark UI Plugin: No active SparkSession found, skipping SQL listener registration")
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
            UITabUtils.addCustomUITab(ui)
            println("Spark UI Plugin: Custom UI tab added successfully")
          } else {
            println("Spark UI Plugin: SparkContext is not available or stopped, skipping UI tab addition")
          }
        } catch {
          case e: Exception =>
            println(s"Spark UI Plugin: Could not add UI tab in thread: ${e.getMessage}")
        }
      }).start()
    } catch {
      case e: Exception =>
        println(s"Spark UI Plugin: Could not add UI tab: ${e.getMessage}")
        e.printStackTrace()
    }
    
    println("Spark UI Plugin: Custom listeners attached successfully")
    
    // Return empty map as required by the interface
    new java.util.HashMap[String, String]()
  }
  
  def addCustomUITab(ui: SparkUI): Unit = {
    // Check if Hello tab already exists
    val isHelloTabAlreadyInstalled = ui.getTabs.exists(_.name == "Hello")
    if (isHelloTabAlreadyInstalled) {
      println("Spark UI Plugin: Hello tab is already installed, skipping installation...")
      return
    }
    
    // Create custom tab
    val helloTab = new HelloWorldTab(ui)
    
    // Create custom page
    val helloPage = new HelloWorldPage(helloTab)
    
    // Attach page to tab
    helloTab.attachPage(helloPage)
    
    // Attach tab to SparkUI
    ui.attachTab(helloTab)
    
    println("Spark UI Plugin: Hello World tab attached successfully")
  }
  
  override def shutdown(): Unit = {
    println("Spark UI Plugin: Driver plugin shutdown")
  }
}

/**
 * Executor-side plugin (minimal implementation)
 */
class SparkUIExecutorPlugin extends ExecutorPlugin {
  
  override def init(ctx: PluginContext, extraConf: java.util.Map[String, String]): Unit = {
    println("Spark UI Plugin: Executor plugin initialized")
  }
  
  override def shutdown(): Unit = {
    println("Spark UI Plugin: Executor plugin shutdown")
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
    println(s"[PLUGIN] Hello World! This is a custom Spark plugin.")
    println(s"[PLUGIN] Check the Spark Web UI for the Hello tab!")
    
    // Add custom tab after application starts (ensures it appears last)
    try {
      val scOpt = SparkContext.getActive
      if (scOpt.isDefined && !scOpt.get.isStopped) {
        val ui = scOpt.get.ui.get
        UITabUtils.addCustomUITab(ui)
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
class HelloWorldTab(parent: SparkUI) extends SparkUITab(parent, "hello") {
  override val name = "Hello"
}

/** Custom Web UI Page that displays "Hello, World" */
class HelloWorldPage(parent: HelloWorldTab) extends WebUIPage("") {
  
  override def render(request: javax.servlet.http.HttpServletRequest): Seq[scala.xml.Node] = {
    try {
      val variables = Map(
        "currentTime" -> new java.util.Date().toString
      )
      val htmlContent = TemplateRenderer.renderTemplate("index.html", variables)
      
      // Unparsed를 사용해서 HTML을 그대로 반환
      // Seq(scala.xml.Unparsed(htmlContent))
      NodeSeq.fromSeq(Seq(Unparsed(htmlContent))) // NodeSeq로 반환하지 않으면 "List()"가 겉에 붙는다.
    } catch {
      case e: Exception =>
        // 에러가 발생하면 간단한 HTML 문자열 반환
        val errorHtml = s"""
          <div class="container-fluid">
            <div class="row">
              <div class="col-12">
                <div class="card">
                  <div class="card-header">
                    <h3>Hello World (Error)</h3>
                  </div>
                  <div class="card-body">
                    <h1>Hello, World!</h1>
                    <p>Template rendering failed: ${e.getMessage}</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        """
      NodeSeq.fromSeq(Seq(scala.xml.Unparsed(errorHtml)))
    }
  }
}