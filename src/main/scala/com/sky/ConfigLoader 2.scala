package com.sky

import zio._
import scala.io.Source
import org.json4s._
import org.json4s.jackson.JsonMethods.parse

object ConfigLoader {
  def loadConfigFromFile: Task[String] = {
    Task.effect {
      val source = Source.fromResource("Config.json")
      if (source == null) {
        throw new RuntimeException("Config.json not found in resources!")
      }
      source.mkString
    }
  }

  def parseConfig(config: String): Task[List[String]] = Task.effect {
    val json     = parse(config)
    val elements = (json \\ "ratings").children
    elements.map(_.values.toString)
  }
}
