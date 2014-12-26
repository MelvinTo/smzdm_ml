package me.hatu.smzdm

import org.scalatest.FunSuite
import grizzled.slf4j.Logging
import com.typesafe.config._
import java.io.File
import scala.collection.JavaConversions._

class TestTraining extends FunSuite with Logging {

  def loadArticles : List[Article] = {
    val fileString = getClass.getClassLoader.getResource("articles.json").toURI
    val jsonFile = new File(fileString)
    val config = ConfigFactory.parseFile(jsonFile)
    val articles = config.getConfigList("articles")
    val normalized_articles = articles.toList.map( x => new Article(x.getString("title"), x.getString("content"))).filter(x => x.content.length > 10)
    return normalized_articles
  }

  test("Should be able to run a topic modeling") {
    val list = loadArticles
    list.foreach ( x => {
      if(x.content.length < 10) {
        debug("XXX" + x.content)
      }
    })

    val lda = TopicModel.train(list.take(list.length - 1))
    debug(list.last.content)
    debug(list.last.title)
    TopicModel.predict(lda, list.last)

  }

  test("Should be able to load sample articles") {
    val list = loadArticles
    assert(list.size == 236)
  }
}