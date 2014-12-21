package me.hatu.smzdm

import grizzled.slf4j.Logging
import com.mongodb.casbah.Imports._
import com.typesafe.config._

object ArticleDBManager extends Logging {
  val mongodb_server : String = ConfigFactory.load().getString("smzdm.mongo.server")
  val mongodb_port : Integer = ConfigFactory.load().getInt("smzdm.mongo.port")
  val mongoClient = MongoClient(mongodb_server, mongodb_port)
  val db = mongoClient("smzdm")
  val coll = db("articles")

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