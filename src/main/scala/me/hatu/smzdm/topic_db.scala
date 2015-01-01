package me.hatu.smzdm

import grizzled.slf4j.Logging
import com.mongodb.casbah.Imports._
import com.typesafe.config._

object TopicDBManager extends Logging {
  val db = DBManager.get_db
  val coll = db("topics")   
}