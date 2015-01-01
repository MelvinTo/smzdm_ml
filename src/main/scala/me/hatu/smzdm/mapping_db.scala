package me.hatu.smzdm

import grizzled.slf4j.Logging
import com.mongodb.casbah.Imports._
import com.typesafe.config._

object MappingDBManager extends Logging {
  val db = DBManager.get_db
  val coll = db("mappings")    
  val top = 5

  def store_all_mappings(mappings: List[ArticleTopicsMapping]) {
    mappings.foreach( x => store_mapping(x))
  }

  def clean_all_mappings {
    coll.dropCollection
  }

  def get_top_articles(topic_name: String) : List[(String, Int)] = {
    val exists = topic_name $exists true
    val q = MongoDBObject() ++ exists
    val cursor = coll.find(q).sort(MongoDBObject(topic_name -> -1)).limit(top)
    return cursor.toList.map(x => {
      val article_id = x("article_id").asInstanceOf[String]
      val theta = x(topic_name).asInstanceOf[Int]
      (article_id, theta)
    })
  }

  def exists(article_id: String, topic_name: String) : Boolean = {
    val exists = topic_name $exists true
    val q = MongoDBObject("article_id" -> article_id) ++ exists
    val cursor = coll.findOne(q)
    cursor match {
      case None => 
        debug(f"mapping not exists: $article_id%s - $topic_name%s")
        return false
      case Some(mapping) =>
        debug(f"mapping exists: $article_id%s - $topic_name%s: $mapping%s")
        return true
    }
  }

  def exists(article_id: String, topic_name: String, theta: Int) : Boolean = {
    val q = MongoDBObject("article_id" -> article_id, topic_name -> theta)
    val cursor = coll.findOne(q)
    cursor match {
      case None => 
        debug(f"mapping not exists: $article_id%s - $topic_name%s")
        return false
      case Some(mapping) =>
        debug(f"mapping exists: $article_id%s - $topic_name%s: $mapping%s")
        return true
    }
  }

  def store_mapping(mapping: ArticleTopicsMapping) {

    val builder = coll.initializeUnorderedBulkOperation

    var obj = MongoDBObject("article_id" -> mapping.article_id)

    mapping.topics.foreach{
      case (topic_name, theta) => 
        obj = obj ++ (topic_name -> theta)
    }

    builder.insert(obj)

    try {
      // debug("storing mappings")
      val result = builder.execute()
    } catch {
      case e : com.mongodb.MongoException.DuplicateKey => debug("entry already exists")
      case other : Throwable => error("got error: " + other)
    }

  }
}