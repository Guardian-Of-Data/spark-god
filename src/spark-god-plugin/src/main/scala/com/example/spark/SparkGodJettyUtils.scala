package org.apache.spark

import org.apache.spark.ui.SparkUI
import javax.servlet.Servlet

object SparkGodJettyUtils {
  def addStaticHandler(ui: SparkUI, resourceBase: String, path: String = "/spark-god/assets"): Unit = {
    val handler = createStaticHandler(resourceBase, path)
    val method = ui.getClass.getMethods.filter(s => s.getName == "attachHandler" && s.getParameterCount == 1).head
    method.invoke(ui, handler.asInstanceOf[Object])
  }

  private def createStaticHandler(resourceBase: String, path: String): Any = {
    def getClassForName(className: String): Class[_] = {
      try {
        Class.forName(s"org.sparkproject.jetty.servlet.$className")
      } catch {
        case _: ClassNotFoundException => Class.forName(s"org.eclipse.jetty.servlet.$className")
      }
    }

    val servletContextHandlerClass = getClassForName("ServletContextHandler")
    val defaultServletClass = getClassForName("DefaultServlet")
    val servletHolderClass = getClassForName("ServletHolder")

    val contextHandler = servletContextHandlerClass.getDeclaredConstructor().newInstance()
    val setInitParameterMethod = contextHandler.getClass.getMethod("setInitParameter", classOf[String], classOf[String])
    setInitParameterMethod.invoke(contextHandler, "org.eclipse.jetty.servlet.Default.gzip", "false")

    val staticHandler = defaultServletClass.getDeclaredConstructor().newInstance()
    val servletHolderConstructor = servletHolderClass.getConstructor(classOf[Servlet])
    val holder = servletHolderConstructor.newInstance(staticHandler.asInstanceOf[Object])

    Option(this.getClass.getClassLoader.getResource(resourceBase)) match {
      case Some(res) =>
        val setInitParameterMethodForHolder = holder.getClass.getMethod("setInitParameter", classOf[String], classOf[String])
        setInitParameterMethodForHolder.invoke(holder, "resourceBase", res.toString)
      case None =>
        throw new Exception("Could not find resource path for Web UI: " + resourceBase)
    }

    val setContextPathMethod = contextHandler.getClass.getMethod("setContextPath", classOf[String])
    setContextPathMethod.invoke(contextHandler, path)

    val addServletMethod = contextHandler.getClass.getMethod("addServlet", servletHolderClass, classOf[String])
    addServletMethod.invoke(contextHandler, holder.asInstanceOf[Object], "/")

    contextHandler
  }
}
