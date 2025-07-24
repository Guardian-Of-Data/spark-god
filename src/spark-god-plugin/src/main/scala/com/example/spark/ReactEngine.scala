package org.apache.spark

import java.io.InputStream
import java.nio.charset.StandardCharsets
import scala.io.Source
// import scala.util.{Try, Success, Failure}

/**
 * Utility class for loading and rendering HTML templates
 */
object ReactEngine {
  /**
   * Load HTML template from resources
   */
  def loadTemplate(templateName: String): String = {
    val resourcePath = s"/ui/$templateName"
    val inputStream = getClass.getResourceAsStream(resourcePath)

    if (inputStream == null) {
      throw new RuntimeException(s"Template not found: $resourcePath")
    }

    try {
      Source.fromInputStream(inputStream, StandardCharsets.UTF_8.name()).mkString
    } finally {
      inputStream.close()
    }
  }

  /**
   * Render template with variable substitution
   */
  def renderTemplate(templateName: String, variables: Map[String, String]): String = {
    val template = loadTemplate(templateName)
    var result = template

    variables.foreach { case (key, value) =>
      result = result.replace(s"{{$key}}", value)
    }

    result
  }
}
