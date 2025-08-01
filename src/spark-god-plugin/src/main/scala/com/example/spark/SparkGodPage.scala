package org.apache.spark

import org.apache.spark.ui.{SparkUITab, WebUIPage}
import scala.xml.{Node, NodeSeq, Unparsed}
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

class SparkGodPage(parent: SparkGodTab) extends WebUIPage("") {
  override def render(request: HttpServletRequest): Seq[Node] = {
    val path = Option(request.getPathInfo).getOrElse("")
    val response = request.getAttribute("response").asInstanceOf[HttpServletResponse]

    // 1. 정적 파일 요청이면 파일만 반환
    val staticPrefixes = Seq("/spark-god/assets/", "/spark-god/vite.svg", "/assets/", "/vite.svg")
    val matchedPrefix = staticPrefixes.find(path.startsWith)
    if (matchedPrefix.isDefined) {
      val resourcePath =
        if (path.startsWith("/spark-god/assets/")) s"/ui/assets/${path.stripPrefix("/spark-god/assets/")}"
        else if (path.startsWith("/spark-god/vite.svg")) "/ui/vite.svg"
        else if (path.startsWith("/assets/")) s"/ui/assets/${path.stripPrefix("/assets/")}"
        else if (path.startsWith("/vite.svg")) "/ui/vite.svg"
        else ""
      val stream = getClass.getResourceAsStream(resourcePath)
      if (stream != null && response != null) {
        val mimeType =
          if (resourcePath.endsWith(".js")) "application/javascript"
          else if (resourcePath.endsWith(".css")) "text/css"
          else if (resourcePath.endsWith(".svg")) "image/svg+xml"
          else "application/octet-stream"
        response.setContentType(mimeType)
        val out = response.getOutputStream
        val buffer = new Array[Byte](8192)
        var len = stream.read(buffer)
        while (len != -1) {
          out.write(buffer, 0, len)
          len = stream.read(buffer)
        }
        stream.close()
        out.flush()
        return Seq.empty
      } else if (response != null) {
        response.setStatus(404)
        response.setContentType("text/plain")
        response.getWriter.write("Not Found")
        response.getWriter.flush()
        return Seq.empty
      }
    }

    // 2. /spark-god 또는 /spark-god/ 요청만 index.html 반환
    if (path == "" || path == "/" || path == "/spark-god" || path == "/spark-god/") {
      try {
        val variables = Map(
          "currentTime" -> new java.util.Date().toString
        )
        val htmlContent = ReactEngine.renderTemplate("index.html", variables)
        NodeSeq.fromSeq(Seq(Unparsed(htmlContent)))
      } catch {
        case e: Exception =>
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
    } else {
      // 3. 그 외는 404 반환
      if (response != null) {
        response.setStatus(404)
        response.setContentType("text/plain")
        response.getWriter.write("Not Found")
        response.getWriter.flush()
      }
      Seq.empty
    }
  }
}
 