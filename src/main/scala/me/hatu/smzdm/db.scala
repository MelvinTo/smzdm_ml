package me.hatu.smzdm

import grizzled.slf4j.Logging
import com.mongodb.casbah.Imports._
import com.typesafe.config._

object DBManager extends Logging {
  val mongodb_server : String = ConfigFactory.load().getString("smzdm.mongo.server")
  val mongodb_port : Integer = ConfigFactory.load().getInt("smzdm.mongo.port")
  val mongoClient = MongoClient(mongodb_server, mongodb_port)
  val db = mongoClient("smzdm")

  def get_db: MongoDB = {
    return db
  }
}