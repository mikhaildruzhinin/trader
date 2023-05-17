package com.github.mikhaildruzhinin.trader

import com.typesafe.scalalogging.Logger

import javax.servlet.annotation.WebServlet
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}

@WebServlet(urlPatterns = Array("/"))
class Servlet extends HttpServlet {
  val log: Logger = Logger(getClass.getName.stripSuffix("$"))

  override protected def doGet(req: HttpServletRequest, res: HttpServletResponse): Unit = {
    log.info("{}/ {} {}", req.getMethod, req.getRequestURL.toString, res.getStatus)

    res.setContentType("text/html")
    res.setCharacterEncoding("UTF-8")
    res.getWriter.println(
      """
        |<!DOCTYPE html>
        |<html lang="en">
        |<head>
        |  <title>Trader servlet</title>
        |  <link rel="shortcut icon" href="data:image/x-icon;," type="image/x-icon">
        |</head>
        |<body>
        |  <p>Trader servlet is running</p>
        |</body>
        |</html>
        |""".stripMargin
    )
  }
}
