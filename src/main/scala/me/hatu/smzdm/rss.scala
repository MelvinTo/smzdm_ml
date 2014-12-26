package me.hatu.smzdm


import scala.collection.JavaConversions._
import grizzled.slf4j.Logging

import akka.actor.Actor
import akka.actor.Props
import scala.concurrent.duration._



object FeedParser extends Logging {

  def run {
    debug("Starting another round of smzdm article check")
    val articles = WebParser.load_new_articles
    val size = articles.size
    debug(f"Found $size%d new articles")
    for(article <- articles) {
      debug(article)
      ArticleDBManager.store_article(article)
    }
  }

  def main(args: Array[String]): Unit = {
    // val a = WebParser.parse_article("Pedigree 宝路 成犬全面营养牛肉狗粮 7.5kg", "http://www.smzdm.com/youhui/625415")
    // ArticleDBManager.store_article(a)

    // ArticleDBManager.exists("http://www.smzdm.com/youhui/625415")
    // ArticleDBManager.exists("http://www.smzdm.com/youhui/625415f")
    val system = akka.actor.ActorSystem("system")

    import system.dispatcher

    system.scheduler.schedule(0 seconds, 5 minutes)(run)
  }
}
