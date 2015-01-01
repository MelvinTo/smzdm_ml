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

    val mappings = TopicModel.get_all_mappings(lda)
    assert(mappings.size == 1484)

    // use last one as testing data
    debug("xxxxxxxxxx test article xxxxxxxx")
    val test_article = list.last
    debug(test_article.title)
    debug(test_article.content)

    val doc = TopicModel.predict(lda, test_article)
    val mapping = TopicModel.get_mapping(doc)
    // debug(mapping)
    val top_topics = TopicModel.get_top_topics(mapping)
    top_topics.foreach(x => debug(x))

    // get related articles
    top_topics.foreach{
      case (topic_name, theta) =>
        val articles = MappingDBManager.get_top_articles(topic_name)
        articles.foreach{
          case (article_id, theta) =>
            debug(f"Similar article: $article_id%s - $theta%d")
        }  
    }
    debug("xxxxxxxxxx end of test article xxxxxxxx")

        

  }

  test("Should be able to get top articles") {
    // get top articles for topic 0
    val articles = MappingDBManager.get_top_articles("0")
    debug(articles)
    assert(articles.size == 5)
    assert(articles(0) == ("怀孕你需要知道的那些事 篇一：十月孕成文武相 124", 377))
  }

  test("Should be able to load sample articles") {
    val list = loadArticles
    assert(list.size == 1485)
  }
}