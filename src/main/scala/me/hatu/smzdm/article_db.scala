package me.hatu.smzdm

import grizzled.slf4j.Logging
import com.mongodb.casbah.Imports._
import com.typesafe.config._

object ArticleDBManager extends Logging {
  val db = DBManager.get_db
  val coll = db("articles")

  def store_articles(articles: List[Article]) {
    val builder = coll.initializeUnorderedBulkOperation
    articles.foreach( article => {
      val obj = MongoDBObject(
        "title" -> article.title,
        "link"  -> article.link,
        "content" -> article.content,
        "category" ->  article.categories,
        "keywords" -> article.keywords
      )
      builder.insert(obj)
    })
    try {
      val result = builder.execute()
    } catch {
      case e : com.mongodb.MongoException.DuplicateKey => debug("entry already exists")
      case other : Throwable => error("got error: " + other)
    }
  }

  def store_article(article: Article) {
    val obj = MongoDBObject(
        "title" -> article.title,
        "link"  -> article.link,
        "content" -> article.content,
        "category" ->  article.categories,
        "keywords" -> article.keywords
    )

    try {
      coll.insert(obj)
    } catch {
      case e : com.mongodb.MongoException.DuplicateKey => debug("entry %s already exists" format (article.link))
      case other : Throwable => error("got error: " + other)
    }
  }

  def exists(link: String) = {
    val q = MongoDBObject("link" -> link)
    val cursor = coll.findOne(q)
    if (cursor == None) {
    	debug("article does not exist: " + link)
    } else {
    	debug("artcile exists: " + link)
    }
//    val article = mongoColl.
  }
}