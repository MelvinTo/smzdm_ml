package me.hatu.smzdm

import com.sun.syndication.io._
import com.sun.syndication.feed.synd._
import scala.collection.JavaConversions._
import grizzled.slf4j.Logging

object FeedParser extends Logging {

  def main(args: Array[String]): Unit = {
    val a = WebParser.parse_article("Pedigree 宝路 成犬全面营养牛肉狗粮 7.5kg", "http://www.smzdm.com/youhui/625415")
    ArticleDBManager.store_article(a)

    ArticleDBManager.exists("http://www.smzdm.com/youhui/625415")
    ArticleDBManager.exists("http://www.smzdm.com/youhui/625415f")
    
  }
}
