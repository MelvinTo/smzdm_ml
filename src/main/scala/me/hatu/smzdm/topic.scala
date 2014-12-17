package me.hatu.smzdm

import grizzled.slf4j.Logging


object TopicModel extends Logging {
	var stopwords : List[String] = List[String]()

	def load_stopwords {
		debug("Load stopwords...")
		stopwords = scala.io.Source.fromFile("stop.lst").mkString.split("\n").toList
	}
	
	def is_stopword(word : String) : Boolean = {
		return stopwords.contains(word)
	}
}